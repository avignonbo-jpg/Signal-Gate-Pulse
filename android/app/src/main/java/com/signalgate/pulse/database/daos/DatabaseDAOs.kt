package com.signalgate.pulse.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.signalgate.pulse.database.entities.CallLogEntry
import com.signalgate.pulse.database.entities.PendingCardEntity
import com.signalgate.pulse.database.entities.SettingEntry
import com.signalgate.pulse.database.entities.SourceEntity
import com.signalgate.pulse.database.entities.SyncHistoryEntry
import com.signalgate.pulse.database.entities.UnifiedEntryEntity
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

    @Query("UPDATE sources SET last_attempted_sync = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun recordSyncAttempt(id: Int, timestamp: Long)

    @Query("UPDATE sources SET last_accepted_snapshot = :timestamp, lastSynced = :timestamp, entriesCount = :entriesCount, healthStatus = 'HEALTHY', updatedAt = :timestamp WHERE id = :id")
    suspend fun recordSnapshotAccepted(id: Int, timestamp: Long, entriesCount: Int)
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

    /**
     * Phase 2.6 — Priority-based conflict resolution.
     *
     * Returns all entries for a given phone number joined with their source's
     * priority, ordered highest priority first. When multiple sources disagree
     * on a number (one BLOCK, one ALLOW), the caller takes the first row —
     * the highest-priority source wins. MANUAL source is seeded at priority 100
     * by DatabaseInitializer, so user decisions always override external sources.
     *
     * The join is on unified_entries.sourceId = sources.id. The query uses
     * a LEFT JOIN so entries with a missing source (shouldn't happen due to FK
     * CASCADE, but defensive) still appear with NULL priority — COALESCE
     * treats them as priority 0, which loses every conflict, which is correct.
     */
    @Query("""
        SELECT ue.* FROM unified_entries ue
        LEFT JOIN sources s ON ue.sourceId = s.id
        WHERE ue.phoneNumber = :phoneNumber
        ORDER BY COALESCE(s.priority, 0) DESC, ue.id ASC
    """)
    suspend fun findEntriesByPhoneNumberWithPriority(phoneNumber: String): List<UnifiedEntryEntity>

    /**
     * Phase 2.6 — Priority-ordered pattern lookup.
     *
     * All BLOCK patterns joined with source priority, highest priority first.
     * Used by getCallDecision() to resolve prefix/area-code blocks correctly
     * when multiple sources define overlapping patterns.
     */
    @Query("""
        SELECT ue.* FROM unified_entries ue
        LEFT JOIN sources s ON ue.sourceId = s.id
        WHERE ue.isPattern = 1 AND ue.action = 'BLOCK'
        ORDER BY COALESCE(s.priority, 0) DESC
    """)
    suspend fun getAllBlockPatternsWithPriority(): List<UnifiedEntryEntity>
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

    @Update
    suspend fun updateCallLog(callLog: CallLogEntry)

    @Delete
    suspend fun deleteCallLog(callLog: CallLogEntry)
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
 * Cards are created on Tier 3 HEURISTIC_BLOCK decisions and deleted on dismissal.
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

    /**
     * Dismisses all undismissed cards matching a phone number.
     * Used by the ACTION_NOT_SPAM broadcast so CallActionReceiver can dismiss
     * the card inline without needing the card's primary key.
     */
    @Query("UPDATE pending_cards SET dismissed = 1 WHERE phoneNumber = :phoneNumber AND dismissed = 0")
    suspend fun dismissByPhoneNumber(phoneNumber: String)

    @Query("DELETE FROM pending_cards WHERE id = :cardId")
    suspend fun deleteCard(cardId: Int)

    @Query("DELETE FROM pending_cards WHERE dismissed = 1")
    suspend fun deleteAllDismissed()

    @Query("DELETE FROM pending_cards")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM pending_cards WHERE dismissed = 0")
    fun getUndismissedCount(): Flow<Int>
}
