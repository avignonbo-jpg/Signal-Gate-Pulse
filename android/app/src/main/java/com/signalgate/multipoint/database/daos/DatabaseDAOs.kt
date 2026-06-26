package com.signalgate.multipoint.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.signalgate.multipoint.database.entities.CallLogEntry
import com.signalgate.multipoint.database.entities.SettingEntry
import com.signalgate.multipoint.database.entities.SourceEntity
import com.signalgate.multipoint.database.entities.SyncHistoryEntry
import com.signalgate.multipoint.database.entities.PendingCardEntity
import com.signalgate.multipoint.database.entities.UnifiedEntryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for SourceEntity operations.
 */
@Dao
interface SourceDao {
    @Insert
    suspend fun insertSource(source: SourceEntity): Long

    @Update
    suspend fun updateSource(source: SourceEntity)

    @Delete
    suspend fun deleteSource(source: SourceEntity)

    @Query("SELECT * FROM sources WHERE id = :id")
    suspend fun getSourceById(id: Int): SourceEntity?

    @Query("SELECT * FROM sources WHERE name = :name LIMIT 1")
    suspend fun getSourceByName(name: String): SourceEntity?

    @Query("SELECT * FROM sources ORDER BY priority DESC, name ASC")
    fun getAllSources(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources WHERE isEnabled = 1 ORDER BY priority DESC")
    fun getEnabledSources(): Flow<List<SourceEntity>>

    @Query("SELECT COUNT(*) FROM sources")
    fun getSourceCount(): Flow<Int>

    @Query("UPDATE sources SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateSourceEnabled(id: Int, isEnabled: Boolean)

    @Query("UPDATE sources SET lastSynced = :timestamp, entriesCount = :entriesCount, healthStatus = :healthStatus WHERE id = :id")
    suspend fun updateSourceSyncStatus(id: Int, timestamp: Long, entriesCount: Int, healthStatus: String)
}

/**
 * DAO for UnifiedEntryEntity operations.
 */
@Dao
interface UnifiedEntryDao {
    @Insert
    suspend fun insertEntry(entry: UnifiedEntryEntity): Long

    @Insert
    suspend fun insertEntries(entries: List<UnifiedEntryEntity>)

    @Update
    suspend fun updateEntry(entry: UnifiedEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: UnifiedEntryEntity)

    @Query("SELECT * FROM unified_entries WHERE phoneNumber = :phoneNumber AND action = 'ALLOW' LIMIT 1")
    suspend fun findUnifiedAllowEntry(phoneNumber: String): UnifiedEntryEntity?

    @Query("SELECT * FROM unified_entries WHERE phoneNumber = :phoneNumber AND action = 'BLOCK' LIMIT 1")
    suspend fun findUnifiedBlockEntry(phoneNumber: String): UnifiedEntryEntity?

    @Query("SELECT * FROM unified_entries WHERE phoneNumber = :phoneNumber")
    suspend fun findEntriesByPhoneNumber(phoneNumber: String): List<UnifiedEntryEntity>

    @Query("SELECT * FROM unified_entries WHERE sourceId = :sourceId")
    suspend fun findEntriesBySourceId(sourceId: Int): List<UnifiedEntryEntity>

    @Query("SELECT COUNT(*) FROM unified_entries WHERE sourceId = :sourceId")
    suspend fun getEntryCountBySourceId(sourceId: Int): Int

    @Query("DELETE FROM unified_entries WHERE sourceId = :sourceId")
    suspend fun deleteEntriesBySourceId(sourceId: Int)

    @Query("SELECT * FROM unified_entries WHERE isPattern = 1 AND action = 'BLOCK'")
    suspend fun getAllBlockPatterns(): List<UnifiedEntryEntity>

    @Query("SELECT COUNT(*) FROM unified_entries")
    fun getTotalEntryCount(): Flow<Int>

    @Query("SELECT * FROM unified_entries")
    suspend fun getAllEntries(): List<UnifiedEntryEntity>

    @Query("SELECT * FROM unified_entries WHERE sourceId = :sourceId")
    suspend fun getAllBySource(sourceId: Int): List<UnifiedEntryEntity>

    @Query("DELETE FROM unified_entries WHERE phoneNumber = :phoneNumber AND sourceId = :sourceId")
    suspend fun deleteEntryByNumberAndSource(phoneNumber: String, sourceId: Int)
}

/**
 * DAO for CallLogEntry operations.
 */
@Dao
interface CallLogDao {
    @Insert
    suspend fun insertCallLog(callLog: CallLogEntry): Long

    @Query("SELECT * FROM call_log ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentCalls(limit: Int = 100): Flow<List<CallLogEntry>>

    @Query("SELECT * FROM call_log WHERE phoneNumber = :phoneNumber ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getCallsByPhoneNumber(phoneNumber: String, limit: Int = 10): List<CallLogEntry>

    @Query("SELECT COUNT(*) FROM call_log WHERE decision = 'BLOCK' AND timestamp >= :startTime")
    suspend fun getBlockedCallsCount(startTime: Long): Int

    @Query("SELECT COUNT(*) FROM call_log WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getCallsInRange(startTime: Long, endTime: Long): Int

    @Query("DELETE FROM call_log WHERE timestamp < :timestamp")
    suspend fun deleteOldCallLogs(timestamp: Long)
}

/**
 * DAO for SettingEntry operations.
 */
@Dao
interface SettingDao {
    @Insert
    suspend fun insertSetting(setting: SettingEntry): Long

    @Update
    suspend fun updateSetting(setting: SettingEntry)

    @Query("SELECT * FROM settings WHERE key = :key")
    suspend fun getSettingByKey(key: String): SettingEntry?

    @Query("SELECT value FROM settings WHERE key = :key")
    suspend fun getSettingValue(key: String): String?

    @Query("UPDATE settings SET value = :value, updatedAt = :timestamp WHERE key = :key")
    suspend fun updateSettingValue(key: String, value: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM settings")
    suspend fun getAllSettings(): List<SettingEntry>
}

/**
 * DAO for SyncHistoryEntry operations.
 */
@Dao
interface SyncHistoryDao {
    @Insert
    suspend fun insertSyncHistory(syncHistory: SyncHistoryEntry): Long

    @Query("SELECT * FROM sync_history WHERE sourceId = :sourceId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getSyncHistoryBySourceId(sourceId: Int, limit: Int = 10): List<SyncHistoryEntry>

    @Query("SELECT * FROM sync_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSyncHistory(limit: Int = 50): List<SyncHistoryEntry>

    @Query("DELETE FROM sync_history WHERE timestamp < :timestamp")
    suspend fun deleteOldSyncHistory(timestamp: Long)
}

/**
 * DAO for PendingCardEntity operations.
 * Cards are created on BLOCK decisions and deleted on dismissal.
 * Never used for permanent history — that is CallLogDao's job.
 */
@Dao
interface PendingCardDao {
    @Insert
    suspend fun insertCard(card: PendingCardEntity): Long

    @Query("SELECT * FROM pending_cards WHERE dismissed = 0 ORDER BY timestamp DESC")
    fun getUndismissedCards(): Flow<List<PendingCardEntity>>

    @Query("UPDATE pending_cards SET dismissed = 1 WHERE id = :cardId")
    suspend fun dismissCard(cardId: Int)

    @Query("DELETE FROM pending_cards WHERE id = :cardId")
    suspend fun deleteCard(cardId: Int)

    @Query("DELETE FROM pending_cards WHERE dismissed = 1")
    suspend fun deleteAllDismissed()

    @Query("DELETE FROM pending_cards")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM pending_cards WHERE dismissed = 0")
    fun getUndismissedCount(): Flow<Int>
}
