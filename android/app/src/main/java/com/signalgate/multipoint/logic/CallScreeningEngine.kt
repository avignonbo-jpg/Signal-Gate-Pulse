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
 *   "manual_allow"  → Tier 1 ALLOWLISTED  — ALLOW, no analysis
 *   "aggregated"    → Tier 2 FEDERAL_BLOCK — BLOCK + setSilenceCall, log only
 *   "manual_block"  → Tier 2 FEDERAL_BLOCK — BLOCK + setSilenceCall, log only
 *   "pattern" (high confidence) → Tier 3 HEURISTIC_BLOCK — BLOCK + notification + PendingCard
 *   "pattern" (low confidence)  → Tier 4 HEURISTIC_FLAG  — SCREEN (ring through), faint log tag
 *   "default"       → Tier 5 CLEAN_UNKNOWN — ALLOW, standard log
 *
 * Step 1.11 will insert real heuristics before the default tier. Until then,
 * Tier 3/4 are populated only by pattern-matched blocks.
 */
class CallScreeningEngine(private val repository: DataSourceRepository) {

    companion object {
        private const val TAG = "CallScreeningEngine"
        private const val HIGH_CONFIDENCE_THRESHOLD = 70
    }

    suspend fun screenCall(phoneNumber: String): CallInfo {
        val normalized = normalizePhoneNumber(phoneNumber)
        Log.d(TAG, "Screening call from: $phoneNumber (normalized: $normalized)")

        return try {
            val decision = repository.getCallDecision(normalized)
            when (decision.action) {
                "ALLOW" -> buildAllowInfo(phoneNumber, normalized, decision)
                "BLOCK" -> buildBlockInfo(phoneNumber, normalized, decision)
                else    -> buildDefaultInfo(phoneNumber, normalized)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Engine error for $phoneNumber, defaulting to ALLOW", e)
            buildDefaultInfo(phoneNumber, normalized)
        }
    }

    // ── Tier builders ─────────────────────────────────────────────────────────

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
            originalPhoneNumber  = original,
            normalizedPhoneNumber = normalized,
            spamStatus           = if (tier == CallTier.ALLOWLISTED) "SAFE" else "UNKNOWN",
            spamCategory         = null,
            confidence           = decision.confidence,
            riskLevel            = "LOW",
            matchedSources       = listOf(decision.reason),
            callDecision         = SignalGateCallScreeningService.CallDecision.ALLOW,
            tier                 = tier
        )
    }

    private fun buildBlockInfo(
        original: String,
        normalized: String,
        decision: DataSourceRepository.CallDecision
    ): CallInfo {
        val isHighConfidence = decision.confidence >= HIGH_CONFIDENCE_THRESHOLD

        val tier = when {
            decision.source == "aggregated"  -> CallTier.FEDERAL_BLOCK
            decision.source == "manual_block" -> CallTier.FEDERAL_BLOCK
            isHighConfidence                  -> CallTier.HEURISTIC_BLOCK
            else                              -> CallTier.HEURISTIC_FLAG
        }

        // Low-confidence match: let the call ring through but flag it in the log.
        val callDecision = if (tier == CallTier.HEURISTIC_FLAG) {
            SignalGateCallScreeningService.CallDecision.SCREEN
        } else {
            SignalGateCallScreeningService.CallDecision.BLOCK
        }

        Log.d(TAG, "$callDecision — tier=$tier source=${decision.source} confidence=${decision.confidence}")

        return CallInfo(
            originalPhoneNumber  = original,
            normalizedPhoneNumber = normalized,
            spamStatus = when (tier) {
                CallTier.FEDERAL_BLOCK    -> "BLOCKED"
                CallTier.HEURISTIC_BLOCK  -> "LIKELY SPAM"
                CallTier.HEURISTIC_FLAG   -> "FLAGGED"
                else                      -> "UNKNOWN"
            },
            spamCategory          = null,
            confidence            = decision.confidence,
            riskLevel             = if (isHighConfidence) "HIGH" else "MEDIUM",
            matchedSources        = listOf(decision.reason),
            callDecision          = callDecision,
            tier                  = tier
        )
    }

    private fun buildDefaultInfo(original: String, normalized: String): CallInfo {
        Log.d(TAG, "ALLOW — Tier 5 CLEAN_UNKNOWN (no match)")
        return CallInfo(
            originalPhoneNumber  = original,
            normalizedPhoneNumber = normalized,
            spamStatus           = "UNKNOWN",
            spamCategory         = null,
            confidence           = null,
            riskLevel            = null,
            matchedSources       = emptyList(),
            callDecision         = SignalGateCallScreeningService.CallDecision.ALLOW,
            tier                 = CallTier.CLEAN_UNKNOWN
        )
    }

    private fun normalizePhoneNumber(phoneNumber: String): String =
        phoneNumber.replace(Regex("[^0-9+]"), "")
}
