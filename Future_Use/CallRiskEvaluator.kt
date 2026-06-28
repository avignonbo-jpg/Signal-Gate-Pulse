app/src/main/java/com/signalgate/logic/CallRiskEvaluator.kt
package com.signalgate.multipoint.logic

import android.os.Build
import android.telecom.Call
import timber.log.Timber

/**
 * CallRiskEvaluator computes a composite risk score for an incoming call.
 *
 * Two inputs feed the score:
 * 1. STIR/SHAKEN attestation — the carrier's identity assertion for the call.
 *    Level A = fully attested (caller ID verified). Level B = partial.
 *    Level C or UNKNOWN = unverified — high-risk signal.
 * 2. Source match count — how many independent data sources flagged this number.
 *    Each independent match adds weight.
 *
 * Output: Int 0–100. Used by HeuristicsEngine as one signal among several.
 * Not used for hard blocking — only advisory confidence contribution.
 *
 * STIR/SHAKEN is available on API 29+ via Call.Details extras.
 * On older APIs this returns UNKNOWN, which adds risk weight as expected.
 *
 * Future_Use file promoted to production — Step 1.11 (Gray-Zone Heuristics).
 */
object CallRiskEvaluator {

    private const val TAG = "CallRiskEvaluator"

    // Score weights
    private const val WEIGHT_PER_SOURCE_MATCH = 25
    private const val WEIGHT_STIR_UNVERIFIED = 35
    private const val WEIGHT_STIR_PARTIAL = 15
    private const val MAX_SOURCE_CONTRIBUTION = 50

    /**
     * Reads STIR/SHAKEN attestation from Call.Details extras.
     * Returns "A", "B", "C", or "UNKNOWN".
     * Only available on API 29+.
     */
    fun getStirAttestation(details: Call.Details?): String {
        if (details == null) return "UNKNOWN"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                details.extras?.getString("stir_attestation") ?: "UNKNOWN"
            } catch (e: Exception) {
                Timber.tag(TAG).w("Failed to read STIR attestation: ${e.message}")
                "UNKNOWN"
            }
        } else {
            "UNKNOWN"
        }
    }

    /**
     * Computes a composite risk score (0–100) from source match count and STIR level.
     *
     * @param sourcesMatched Number of independent sources that flagged this number.
     * @param stirLevel STIR/SHAKEN attestation level: "A", "B", "C", or "UNKNOWN".
     * @return Risk score 0–100. Higher = more suspicious.
     */
    fun calculateRiskScore(sourcesMatched: Int, stirLevel: String): Int {
        var score = 0

        // Source match contribution — capped to prevent a single dimension dominating
        val sourceContribution = (sourcesMatched * WEIGHT_PER_SOURCE_MATCH)
            .coerceAtMost(MAX_SOURCE_CONTRIBUTION)
        score += sourceContribution

        // STIR/SHAKEN contribution
        score += when (stirLevel.uppercase()) {
            "A" -> 0                        // Fully attested — no additional risk
            "B" -> WEIGHT_STIR_PARTIAL      // Partial attestation — moderate signal
            "C", "UNKNOWN" -> WEIGHT_STIR_UNVERIFIED  // Unverified — high-risk signal
            else -> WEIGHT_STIR_UNVERIFIED
        }

        val finalScore = score.coerceIn(0, 100)
        Timber.tag(TAG).d(
            "Risk score: $finalScore (sources=$sourcesMatched, stir=$stirLevel)"
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
    val stirLevel: String,     // "A", "B", "C", "UNKNOWN"
    val sourcesMatched: Int,   // independent source hits
    val riskLevel: String      // "HIGH", "MEDIUM", "LOW"
)
