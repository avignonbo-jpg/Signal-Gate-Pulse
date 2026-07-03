package com.signalgate.multipoint.database.repositories

import com.signalgate.multipoint.database.daos.CallLogDao
import com.signalgate.multipoint.database.entities.CallLogEntry
import kotlinx.coroutines.flow.Flow

class CallLogRepository(private val callLogDao: CallLogDao) {
    val allLogsFlow: Flow<List<CallLogEntry>> = callLogDao.getRecentCalls(100)

    suspend fun insertCallLog(entry: CallLogEntry) {
        callLogDao.insertCallLog(entry)
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
