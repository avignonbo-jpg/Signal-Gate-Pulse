package com.signalgate.pulse.logic

import android.os.Build
import android.telecom.Call
import android.telecom.Connection
import timber.log.Timber

/**
 * CallRiskEvaluator computes a composite risk score for an incoming call.
 *
 * Two inputs feed the score:
 * 1. Caller number verification status — the network's STIR/SHAKEN verdict for the call,
 *    surfaced to CallScreeningService apps via Call.Details.getCallerNumberVerificationStatus()
 *    on API 30+ (Android 11). Android does NOT expose full A/B/C attestation levels to apps —
 *    only a three-state verdict: PASSED, FAILED, or NOT_VERIFIED. FAILED is treated as the
 *    strongest spoofing signal (the network actively could not confirm the caller ID);
 *    NOT_VERIFIED means no carrier in the path supported/attempted verification.
 * 2. Source match count — how many independent data sources flagged this number.
 *    Each independent match adds weight.
 *
 * Output: Int 0–100. Used by HeuristicsEngine as one signal among several.
 * Not used for hard blocking — only advisory confidence contribution.
 *
 * Verification status is available on API 30+ via Call.Details.getCallerNumberVerificationStatus().
 * On older APIs (or when unavailable) this returns UNKNOWN, which adds risk weight as expected.
 *
 * Future_Use file promoted to production — Step 1.11 (Gray-Zone Heuristics).
 */
object CallRiskEvaluator {

    private const val TAG = "CallRiskEvaluator"

    // Score weights
    private const val WEIGHT_PER_SOURCE_MATCH = 25
    private const val WEIGHT_STIR_NOT_VERIFIED = 35
    private const val WEIGHT_STIR_FAILED = 50
    private const val MAX_SOURCE_CONTRIBUTION = 50

    /**
     * Reads the network's caller-ID verification verdict from Call.Details.
     * Returns "PASSED", "FAILED", "NOT_VERIFIED", or "UNKNOWN".
     *
     * Only available on API 30+ (Android 11) via getCallerNumberVerificationStatus().
     * Below that, or if details is null, returns "UNKNOWN" — no signal either way.
     *
     * Note: Android intentionally does not expose STIR/SHAKEN attestation levels (A/B/C)
     * to third-party apps — only this coarser three-state verdict.
     */
    fun getStirAttestation(details: Call.Details?): String {
        if (details == null) return "UNKNOWN"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return "UNKNOWN"

        return try {
            when (details.callerNumberVerificationStatus) {
                Connection.VERIFICATION_STATUS_PASSED -> "PASSED"
                Connection.VERIFICATION_STATUS_FAILED -> "FAILED"
                Connection.VERIFICATION_STATUS_NOT_VERIFIED -> "NOT_VERIFIED"
                else -> "UNKNOWN"
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w("Failed to read caller number verification status: ${e.message}")
            "UNKNOWN"
        }
    }

    /**
     * Computes a composite risk score (0–100) from source match count and verification status.
     *
     * @param sourcesMatched Number of independent sources that flagged this number.
     * @param stirLevel Verification verdict: "PASSED", "FAILED", "NOT_VERIFIED", or "UNKNOWN".
     * @return Risk score 0–100. Higher = more suspicious.
     */
    fun calculateRiskScore(sourcesMatched: Int, stirLevel: String): Int {
        var score = 0

        // Source match contribution — capped to prevent a single dimension dominating
        val sourceContribution = (sourcesMatched * WEIGHT_PER_SOURCE_MATCH)
            .coerceAtMost(MAX_SOURCE_CONTRIBUTION)
        score += sourceContribution

        // Verification-status contribution
        score += when (stirLevel.uppercase()) {
            "PASSED" -> 0                                  // Network verified caller ID — no additional risk
            "FAILED" -> WEIGHT_STIR_FAILED                 // Network actively flagged spoofing — strongest signal
            "NOT_VERIFIED", "UNKNOWN" -> WEIGHT_STIR_NOT_VERIFIED  // No verification available — moderate risk
            else -> WEIGHT_STIR_NOT_VERIFIED
        }

        val finalScore = score.coerceIn(0, 100)
        Timber.tag(TAG).d(
            "Risk score: $finalScore (sources=$sourcesMatched, verification=$stirLevel)"
        )
        return finalScore
    }

    /**
     * Convenience wrapper that accepts a phone number string and pre-resolved
     * source match count. Call.Details is optional — pass null if not available
     * (e.g. in unit tests or when called from outside a screen context).
     */
    fun evaluate(
        sourcesMatched: Int,
        callDetails: Call.Details? = null
    ): RiskEvaluation {
        val stirLevel = getStirAttestation(callDetails)
        val score = calculateRiskScore(sourcesMatched, stirLevel)
        return RiskEvaluation(
            score = score,
            stirLevel = stirLevel,
            sourcesMatched = sourcesMatched,
            riskLevel = when {
                score >= 70 -> "HIGH"
                score >= 40 -> "MEDIUM"
                else -> "LOW"
            }
        )
    }
}

/**
 * Typed result from CallRiskEvaluator.evaluate().
 * Passed into HeuristicsEngine for tier classification.
 */
data class RiskEvaluation(
    val score: Int,            // 0–100 composite score
    val stirLevel: String,     // "PASSED", "FAILED", "NOT_VERIFIED", "UNKNOWN"
    val sourcesMatched: Int,   // independent source hits
    val riskLevel: String      // "HIGH", "MEDIUM", "LOW"
)
