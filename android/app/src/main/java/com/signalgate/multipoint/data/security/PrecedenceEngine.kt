package com.signalgate.multipoint.data.security

import com.signalgate.multipoint.data.models.CallType

class PrecedenceEngine(
    private val bloomFilter: BloomFilterEngine,
    private val localAllowListCache: HashSet<String>,  // Tier 1: In-Memory Quick-Cache
    private val localManualBlockListCache: HashSet<String>  // Tier 2: Manual Rules
) {
    /**
     * Evaluates incoming phone threat matrices. Implements the absolute rule:
     * TIER 1 (Allow-list) TRUMPS ALL, breaking out immediately before executing database disk operations.
     */
    fun evaluateIncomingCall(incomingNumber: String, onQueryDatabaseVerification: (String) -> Boolean): CallType {
        val cleanNumber = SanitizationEngine.sanitizePhoneNumber(incomingNumber)
        if (cleanNumber.isEmpty()) return CallType.INCOMING

        // 1. Tier 1: Instant Memory Verification (Allow-list Trumps All)
        if (localAllowListCache.contains(cleanNumber)) {
            return CallType.INCOMING  // Explicit bypass allowed
        }

        // 2. Tier 2: Instant Memory Verification (Local User Blocked Rules)
        if (localManualBlockListCache.contains(cleanNumber)) {
            return CallType.BLOCKED
        }

        // 3. Tier 4: In-Memory Bloom Filter Evaluation
        val mightBeSpam = bloomFilter.mightContain(cleanNumber)
        if (!mightBeSpam) {
            return CallType.INCOMING  // 100% Definitive Clean Result (Bypassed disk entirely)
        }

        // 4. Verification Check: Confirm the match against the localized persistent SQLite table
        val isConfirmedSpam = onQueryDatabaseVerification(cleanNumber)
        return if (isConfirmedSpam) CallType.SPAM else CallType.INCOMING
    }
}
