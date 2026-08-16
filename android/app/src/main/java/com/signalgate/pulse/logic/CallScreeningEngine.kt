package com.signalgate.pulse.logic

import timber.log.Timber
import com.signalgate.pulse.CallInfo
import com.signalgate.pulse.CallTier
import com.signalgate.pulse.SignalGateCallScreeningService
import com.signalgate.pulse.data.security.SanitizationEngine
import com.signalgate.pulse.database.repositories.DataSourceRepository
import com.signalgate.pulse.database.repositories.HeuristicsMode
import com.signalgate.pulse.database.repositories.SettingKeys
import com.signalgate.pulse.database.repositories.SettingRepository

/**
 * CallScreeningEngine implements the five-tier Priority Hierarchy.
 *
 * Decision sources from DataSourceRepository.getCallDecision():
 *   "manual_allow"  → Tier 1 ALLOWLISTED  — ALLOW, no further analysis
 *   "aggregated"    → Tier 2 FEDERAL_BLOCK — BLOCK + setSilenceCall, log only
 *   "manual_block"  → Tier 2 FEDERAL_BLOCK — BLOCK + setSilenceCall, log only
 *   "pattern" (confidence >= 70) → Tier 3 HEURISTIC_BLOCK — BLOCK + notification + PendingCard
 *   "pattern" (confidence  < 70) → Tier 4 HEURISTIC_FLAG  — SCREEN (ring through), faint log
 *   "default"  → CallRiskEvaluator gray-zone check → Tier 4 HEURISTIC_FLAG or Tier 5 CLEAN_UNKNOWN
 *
 * Step 1.11 — CallRiskEvaluator wired at gray-zone boundary:
 * When no database rule matches, CallRiskEvaluator.evaluate() provides an advisory
 * risk score from STIR/SHAKEN attestation and source-match count. A score at or
 * above the user's configured heuristics threshold elevates the call to
 * HEURISTIC_FLAG (rings through + digest entry for user review). Below threshold:
 * CLEAN_UNKNOWN (allow, no card).
 *
 * Onboarding Step 3 — protection level now user-configurable via HeuristicsMode
 * (SettingKeys.HEURISTICS_MODE), read fresh on every screenCall() rather than a
 * hardcoded constant. OFF skips CallRiskEvaluator entirely (every gray-zone call
 * is CLEAN_UNKNOWN); CONSERVATIVE/BALANCED/AGGRESSIVE set the score threshold —
 * see HeuristicsMode in SettingKeys.kt for the exact numbers. Falls back to
 * HeuristicsMode.DEFAULT (BALANCED, threshold 55) if the setting is unreadable
 * or unset, so a DB error here degrades to the old fixed behavior rather than
 * disabling heuristics silently.
 *
 * Architecture Contract §6 L6 constraint — enforced here:
 * CallRiskEvaluator's score is advisory INPUT into the decision, never the decision
 * itself. Gray-zone calls are never silently blocked — the user always gets to review
 * them via the digest. Only Tiers 2 and 3 block calls outright.
 */
class CallScreeningEngine(
    private val repository: DataSourceRepository,
    private val riskEvaluator: CallRiskEvaluator = CallRiskEvaluator,
    private val settingRepository: SettingRepository? = null
) {

    companion object {
        private const val TAG = "CallScreeningEngine"
        private const val HIGH_CONFIDENCE_THRESHOLD = 70
    }

    /**
     * Security fix (audit finding): [phoneNumber] here is the raw caller-ID string
     * read straight off android.telecom.Call.Details — the single most externally-
     * controlled input in this app (caller ID / CLI is attacker-spoofable). It is
     * now sanitized via SanitizationEngine.sanitizePhoneNumber() immediately, before
     * being placed into CallInfo.originalPhoneNumber. Previously the raw value was
     * carried unsanitized all the way into CallLogEntry (an Entity, via
     * SignalGateCallScreeningService.writeAuditRecords) and into the blocked-call
     * notification body text — this closes both gaps at the source instead of
     * patching each downstream consumer individually.
     */
    suspend fun screenCall(phoneNumber: String, callDetails: android.telecom.Call.Details?): CallInfo {
        val sanitizedOriginal = SanitizationEngine.sanitizePhoneNumber(phoneNumber)
        val normalized = normalizePhoneNumber(sanitizedOriginal)
        Timber.d("Screening call from: $sanitizedOriginal (normalized: $normalized)")

        return try {
            val decision = repository.getCallDecision(normalized)
            when (decision.action) {
                "ALLOW" -> buildAllowInfo(sanitizedOriginal, normalized, decision)
                "BLOCK" -> buildBlockInfo(sanitizedOriginal, normalized, decision)
                else    -> buildGrayZoneInfo(sanitizedOriginal, normalized, callDetails)
            }
        } catch (e: Exception) {
            Timber.e(e, "Engine error for $sanitizedOriginal, defaulting to ALLOW")
            buildDefaultInfo(sanitizedOriginal, normalized)
        }
    }

    // ── Tier builders ──────────────────────────────────────────────────────────

    private fun buildAllowInfo(
        original: String,
        normalized: String,
        decision: DataSourceRepository.CallDecision
    ): CallInfo {
        val tier = when (decision.source) {
            "manual_allow" -> CallTier.ALLOWLISTED
            else           -> CallTier.CLEAN_UNKNOWN
        }
        Timber.d("ALLOW — tier=$tier source=${decision.source}")
        return CallInfo(
            originalPhoneNumber   = original,
            normalizedPhoneNumber = normalized,
            spamStatus            = if (tier == CallTier.ALLOWLISTED) "SAFE" else "UNKNOWN",
            spamCategory          = null,
            confidence            = decision.confidence,
            riskLevel             = "LOW",
            matchedSources        = listOf(decision.reason),
            callDecision          = SignalGateCallScreeningService.CallDecision.ALLOW,
            tier                  = tier
        )
    }

    private fun buildBlockInfo(
        original: String,
        normalized: String,
        decision: DataSourceRepository.CallDecision
    ): CallInfo {
        val isHighConfidence = decision.confidence >= HIGH_CONFIDENCE_THRESHOLD

        val tier = when {
            decision.source == "aggregated"   -> CallTier.FEDERAL_BLOCK
            decision.source == "manual_block" -> CallTier.FEDERAL_BLOCK
            isHighConfidence                  -> CallTier.HEURISTIC_BLOCK
            else                              -> CallTier.HEURISTIC_FLAG
        }

        val callDecision = if (tier == CallTier.HEURISTIC_FLAG) {
            SignalGateCallScreeningService.CallDecision.SCREEN
        } else {
            SignalGateCallScreeningService.CallDecision.BLOCK
        }

        Timber.d("$callDecision — tier=$tier source=${decision.source} confidence=${decision.confidence}")

        return CallInfo(
            originalPhoneNumber   = original,
            normalizedPhoneNumber = normalized,
            spamStatus = when (tier) {
                CallTier.FEDERAL_BLOCK   -> "BLOCKED"
                CallTier.HEURISTIC_BLOCK -> "LIKELY SPAM"
                CallTier.HEURISTIC_FLAG  -> "FLAGGED"
                else                     -> "UNKNOWN"
            },
            spamCategory          = null,
            confidence            = decision.confidence,
            riskLevel             = if (isHighConfidence) "HIGH" else "MEDIUM",
            matchedSources        = listOf(decision.reason),
            callDecision          = callDecision,
            tier                  = tier
        )
    }

    /**
     * Gray-zone path — no database rule matched.
     * CallRiskEvaluator provides an advisory score from STIR/SHAKEN attestation.
     * sourcesMatched=0 here because the repository found no hits — the STIR signal
     * alone determines whether this is flagged or allowed through cleanly.
     *
     * Score >= threshold (from the user's HeuristicsMode) → HEURISTIC_FLAG: rings through + digest card.
     * Score <  threshold → CLEAN_UNKNOWN: allow, no card.
     */
    private suspend fun buildGrayZoneInfo(
        original: String,
        normalized: String,
        callDetails: android.telecom.Call.Details?
    ): CallInfo {
        // Read fresh every call rather than caching in a field: onboarding Step 3
        // (or a future Settings toggle) can change this mid-session, and this is
        // cheap — a single indexed key lookup — compared to the cost of a stale
        // protection level silently persisting until process restart.
        // Phase 0.6 scope note: this catch block is intentionally NOT a SECURITY_FAILURE
        // candidate and must not be converted to return SECURITY_FAILURE when 0.6 lands.
        // A failure to read heuristics_mode is a settings-read failure, not a security
        // subsystem failure — the engine can still produce a fully correct decision using
        // HeuristicsMode.DEFAULT (BALANCED, threshold 55). Degrading gracefully to the
        // default protection level is correct here. This is a different class of failure
        // from the two catch blocks 0.6 does target: screenCall()'s outer engine catch and
        // onScreenCall()'s service catch, both of which convert a real engine failure into
        // an indistinguishable ALLOW/CLEAN_UNKNOWN result. This one does not — it only
        // affects which gray-zone threshold is applied, not whether the engine runs at all.
        val mode = try {
            HeuristicsMode.fromKey(settingRepository?.getSettingValue(SettingKeys.HEURISTICS_MODE))
        } catch (e: Exception) {
            Timber.e(e, "Failed to read heuristics_mode, defaulting to ${HeuristicsMode.DEFAULT}")
            HeuristicsMode.DEFAULT
        }

        val threshold = mode.riskThreshold
        if (threshold == null) {
            // OFF — don't even run the evaluator.
            Timber.d("Heuristics OFF — skipping gray-zone evaluation")
            return buildDefaultInfo(original, normalized)
        }

        val evaluation = riskEvaluator.evaluate(sourcesMatched = 0, callDetails = callDetails)
        Timber.d("Gray-zone: risk=${evaluation.score} stir=${evaluation.stirLevel} mode=$mode threshold=$threshold")

        return if (evaluation.score >= threshold) {
            Timber.d("SCREEN — Tier 4 HEURISTIC_FLAG (gray-zone risk=${evaluation.score})")
            CallInfo(
                originalPhoneNumber   = original,
                normalizedPhoneNumber = normalized,
                spamStatus            = "CAUTION",
                spamCategory          = null,
                confidence            = evaluation.score,
                riskLevel             = evaluation.riskLevel,
                matchedSources        = listOf("Gray-zone heuristics (STIR: ${evaluation.stirLevel})"),
                callDecision          = SignalGateCallScreeningService.CallDecision.SCREEN,
                tier                  = CallTier.HEURISTIC_FLAG
            )
        } else {
            buildDefaultInfo(original, normalized)
        }
    }

    private fun buildDefaultInfo(original: String, normalized: String): CallInfo {
        Timber.d("ALLOW — Tier 5 CLEAN_UNKNOWN (no match, low risk)")
        return CallInfo(
            originalPhoneNumber   = original,
            normalizedPhoneNumber = normalized,
            spamStatus            = "UNKNOWN",
            spamCategory          = null,
            confidence            = null,
            riskLevel             = null,
            matchedSources        = emptyList(),
            callDecision          = SignalGateCallScreeningService.CallDecision.ALLOW,
            tier                  = CallTier.CLEAN_UNKNOWN
        )
    }

    /**
     * Note: [phoneNumber] arrives here already passed through
     * SanitizationEngine.sanitizePhoneNumber() by screenCall(). This regex further
     * strips wildcard/extension characters (*, #, x) that sanitizePhoneNumber
     * intentionally preserves, since exact-match DB lookups need digits/+ only.
     */
    private fun normalizePhoneNumber(phoneNumber: String): String =
        SanitizationEngine.sanitizePhoneNumber(phoneNumber).replace(Regex("[^0-9+]"), "")
}
