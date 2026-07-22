package com.signalgate.multipoint.database.repositories

import com.signalgate.multipoint.database.daos.SyncHistoryDao
import com.signalgate.multipoint.database.entities.SyncHistoryEntry

/**
 * SyncHistoryRepository wraps [SyncHistoryDao] and provides the production data path
 * for all sync-history read and write operations.
 */
class SyncHistoryRepository(private val syncHistoryDao: SyncHistoryDao) {

    suspend fun insertSyncHistory(entry: SyncHistoryEntry) {
        syncHistoryDao.insertSyncHistory(entry)
    }

    suspend fun getSyncHistoryBySourceId(sourceId: Int, limit: Int = 10): List<SyncHistoryEntry> {
        return syncHistoryDao.getSyncHistoryBySourceId(sourceId, limit)
    }

    suspend fun getRecentSyncHistory(limit: Int = 50): List<SyncHistoryEntry> {
        return syncHistoryDao.getRecentSyncHistory(limit)
    }

    suspend fun deleteOldSyncHistory(beforeTimestamp: Long) {
        syncHistoryDao.deleteOldSyncHistory(beforeTimestamp)
    }
}
