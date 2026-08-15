package com.signalgate.pulse.database.repositories

import com.signalgate.pulse.data.security.SanitizationEngine
import com.signalgate.pulse.database.daos.CallLogDao
import com.signalgate.pulse.database.entities.CallLogEntry
import kotlinx.coroutines.flow.Flow

class CallLogRepository(private val callLogDao: CallLogDao) {
    val allLogsFlow: Flow<List<CallLogEntry>> = callLogDao.getRecentCalls(100)

    /**
     * Security fix (audit finding): this is the write chokepoint for CallLogEntry,
     * which carries phoneNumber/normalizedPhoneNumber sourced from the raw telecom
     * caller-ID (the most externally-controlled, attacker-spoofable input in the
     * app). The caller (SignalGateCallScreeningService) now sanitizes upstream via
     * CallScreeningEngine, but this repository — as the actual Entity write
     * boundary — must not depend on that discipline being maintained by every
     * future caller. sanitizePhoneNumber() is idempotent (pure character removal
     * plus a length cap), so re-applying it here is always safe, never double-escapes.
     */
    suspend fun insertCallLog(entry: CallLogEntry) {
        val sanitized = entry.copy(
            phoneNumber = SanitizationEngine.sanitizePhoneNumber(entry.phoneNumber),
            normalizedPhoneNumber = SanitizationEngine.sanitizePhoneNumber(entry.normalizedPhoneNumber)
        )
        callLogDao.insertCallLog(sanitized)
    }

    suspend fun getCallsByPhoneNumber(phoneNumber: String): List<CallLogEntry> {
        return callLogDao.getCallsByPhoneNumber(phoneNumber)
    }

    suspend fun updateCallLog(entry: CallLogEntry) {
        callLogDao.updateCallLog(entry)
    }

    suspend fun deleteCallLog(entry: CallLogEntry) {
        callLogDao.deleteCallLog(entry)
    }

    suspend fun getBlockedCallsCount(startTime: Long): Int {
        return callLogDao.getBlockedCallsCount(startTime)
    }

    suspend fun getCallsInRange(startTime: Long, endTime: Long): Int {
        return callLogDao.getCallsInRange(startTime, endTime)
    }
}
