package com.signalgate.logic

enum class RiskLevel { LOW, MEDIUM, HIGH }

object RiskThresholdEngine {
    fun getDefault() = RiskLevel.MEDIUM
    
    fun shouldBlock(number: String, riskLevel: RiskLevel, matchedSources: Int, stirAttestation: String?): Boolean {
        return when (riskLevel) {
            RiskLevel.LOW -> matchedSources >= 3
            RiskLevel.MEDIUM -> matchedSources >= 1 || stirAttestation == "C"
            RiskLevel.HIGH -> true // Block most unknowns
        }
    }
}
