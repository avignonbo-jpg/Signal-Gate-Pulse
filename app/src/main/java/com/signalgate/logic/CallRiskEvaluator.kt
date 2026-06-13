package com.signalgate.logic

import android.os.Bundle

object CallRiskEvaluator {
    fun getStirAttestation(callDetails: Bundle?): String {
        // Android exposes via call extras in newer APIs
        return callDetails?.getString("stir_attestation") ?: "UNKNOWN"
    }

    fun calculateRiskScore(number: String, sourcesMatched: Int, stirLevel: String): Int {
        var score = sourcesMatched * 30
        if (stirLevel == "C" || stirLevel == "UNKNOWN") score += 40
        return score.coerceIn(0, 100)
    }
}
