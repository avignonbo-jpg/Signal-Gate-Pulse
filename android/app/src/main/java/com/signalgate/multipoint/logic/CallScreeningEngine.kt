package com.signalgate.multipoint.logic

import android.util.Log
import com.signalgate.multipoint.CallInfo
import com.signalgate.multipoint.CallTier
import com.signalgate.multipoint.SignalGateCallScreeningService
import com.signalgate.multipoint.database.repositories.DataSourceRepository

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
 * above HEURISTIC_RISK_THRESHOLD elevates the call to HEURISTIC_FLAG (rings through
 * + digest entry for user review). Below threshold: CLEAN_UNKNOWN (allow, no card).
 *
 * Architecture Contract §6 L6 constraint — enforced here:
 * CallRiskEvaluator's score is advisory INPUT into the decision, never the decision
 * itself. Gray-zone calls are never silently blocked — the user always gets to review
 * them via the digest. Only Tiers 2 and 3 block calls outright.
 */
class CallScreeningEngine(
    private val repository: DataSourceRepository,
    private val riskEvaluator: CallRiskEvaluator = CallRiskEvaluator
) {

    companion object {
        private const val TAG = "CallScreeningEngine"
        private const val HIGH_CONFIDENCE_THRESHOLD = 70
        private const val HEURISTIC_RISK_THRESHOLD = 55
    }

    suspend fun screenCall(phoneNumber: String): CallInfo {
        val normalized = normalizePhoneNumber(phoneNumber)
        Log.d(TAG, "Screening call from: $phoneNumber (normalized: $normalized)")

        return try {
            val decision = repository.getCallDecision(normalized)
            when (decision.action) {
                "ALLOW" -> buildAllowInfo(phoneNumber, normalized, decision)
                "BLOCK" -> buildBlockInfo(phoneNumber, normalized, decision)
                else    -> buildGrayZoneInfo(phoneNumber, normalized)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Engine error for $phoneNumber, defaulting to ALLOW", e)
            buildDefaultInfo(phoneNumber, normalized)
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
        Log.d(TAG, "ALLOW — tier=$tier source=${decision.source}")
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

        Log.d(TAG, "$callDecision — tier=$tier source=${decision.source} confidence=${decision.confidence}")

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
     * Score >= HEURISTIC_RISK_THRESHOLD → HEURISTIC_FLAG: rings through + digest card.
     * Score <  HEURISTIC_RISK_THRESHOLD → CLEAN_UNKNOWN: allow, no card.
     *
     * Call.Details is not available at this layer — pass null so the evaluator uses
     * STIR-only scoring. Future wiring: pass Call.Details from CallScreeningService
     * into screenCall() to enable full STIR + source-match scoring.
     */
    private fun buildGrayZoneInfo(original: String, normalized: String): CallInfo {
        val evaluation = riskEvaluator.evaluate(sourcesMatched = 0, callDetails = null)
        Log.d(TAG, "Gray-zone: risk=${evaluation.score} stir=${evaluation.stirLevel}")

        return if (evaluation.score >= HEURISTIC_RISK_THRESHOLD) {
            Log.d(TAG, "SCREEN — Tier 4 HEURISTIC_FLAG (gray-zone risk=${evaluation.score})")
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
        Log.d(TAG, "ALLOW — Tier 5 CLEAN_UNKNOWN (no match, low risk)")
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

    private fun normalizePhoneNumber(phoneNumber: String): String =
        phoneNumber.replace(Regex("[^0-9+]"), "")
}
