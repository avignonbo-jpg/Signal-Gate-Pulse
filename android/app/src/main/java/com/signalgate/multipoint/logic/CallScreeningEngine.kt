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
 *   "pattern" (high confidence) → Tier 3 HEURISTIC_BLOCK — BLOCK + notification + PendingCard
 *   "pattern" (low confidence)  → Tier 4 HEURISTIC_FLAG  — SCREEN (ring through), faint log
 *   "default"       → gray-zone heuristics via CallRiskEvaluator → Tier 5 CLEAN_UNKNOWN
 *
 * CallRiskEvaluator (Step 1.8 — wired in this revision):
 * Provides advisory input at the gray-zone boundary only. It does not produce
 * a blocking decision — per Architecture Contract §6 L6, its score is input
 * *into* the engine decision, never the decision itself. A score above
 * HEURISTIC_RISK_THRESHOLD elevates an otherwise CLEAN_UNKNOWN to HEURISTIC_FLAG
 * (ring through + digest entry), never a silent block — the user always gets to
 * review gray-zone calls, never has them silently discarded.
 */
class CallScreeningEngine(
    private val repository: DataSourceRepository,
    private val riskEvaluator: CallRiskEvaluator
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
     * Gray-zone path — no rule matched in the database.
     * CallRiskEvaluator provides an advisory score based on source-match count
     * and STIR/SHAKEN attestation (when available via Call.Details).
     *
     * Score >= HEURISTIC_RISK_THRESHOLD: elevate to HEURISTIC_FLAG (ring through
     * + digest entry for user review). Never silently blocked — Contract §6 L6.
     * Score < HEURISTIC_RISK_THRESHOLD: CLEAN_UNKNOWN, standard allow.
     *
     * Call.Details is not available at this layer — pass sourcesMatched = 0 so
     * the evaluator uses STIR-only scoring. When CallScreeningService passes
     * Call.Details into the engine (future wiring point), sourcesMatched will
     * reflect the actual source hits from the repository decision.
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
