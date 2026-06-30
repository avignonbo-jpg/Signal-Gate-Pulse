package com.signalgate.multipoint.database.repositories

import com.signalgate.multipoint.database.daos.SourceDao
import com.signalgate.multipoint.database.daos.UnifiedEntryDao
import com.signalgate.multipoint.database.entities.SourceEntity
import com.signalgate.multipoint.database.entities.UnifiedEntryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataSourceRepository handles all data source operations and state management.
 * Acts as a single source of truth for data source data.
 */
class DataSourceRepository(
    private val sourceDao: SourceDao,
    private val entryDao: UnifiedEntryDao
) {

    fun getAllSources(): Flow<List<SourceEntity>> {
        return sourceDao.getAllSources()
    }

    fun getEnabledSources(): Flow<List<SourceEntity>> {
        return sourceDao.getEnabledSources()
    }

    fun getSourceCount(): Flow<Int> {
        return sourceDao.getSourceCount()
    }

    fun getEnabledSourceCount(): Flow<Int> {
        return getAllSources().map { sources ->
            sources.count { it.isEnabled }
        }
    }

    suspend fun getSourceById(id: Int): SourceEntity? {
        return sourceDao.getSourceById(id)
    }

    /**
     * Looks up a source by exact name via a direct indexed DAO query.
     *
     * Bug fix (2026-06): previously callers used a private extension that
     * called getAllSources().collect { ... } — Room's Flow from a live query
     * never completes on its own, so collect{} suspended forever. The first
     * caller (ReliableSourceManager.ensureSourceRow) would deadlock its
     * coroutine indefinitely; CommunitySyncWorker would eventually be killed
     * by WorkManager's execution timeout and silently retried forever.
     *
     * SourceDao.getSourceByName() is a one-shot suspend query — it returns
     * once and only once. Always use this method for single-lookup-by-name,
     * never a Flow collect, for exactly this reason.
     */
    suspend fun getSourceByName(name: String): SourceEntity? {
        return sourceDao.getSourceByName(name)
    }

    suspend fun insertSource(source: SourceEntity): Long {
        return sourceDao.insertSource(source)
    }

    suspend fun updateSource(source: SourceEntity) {
        sourceDao.updateSource(source)
    }

    suspend fun deleteSource(source: SourceEntity) {
        sourceDao.deleteSource(source)
    }

    suspend fun toggleSourceEnabled(sourceId: Int, isEnabled: Boolean) {
        sourceDao.updateSourceEnabled(sourceId, isEnabled)
    }

    suspend fun updateSourceSyncStatus(
        sourceId: Int,
        timestamp: Long,
        entriesCount: Int,
        healthStatus: String
    ) {
        sourceDao.updateSourceSyncStatus(sourceId, timestamp, entriesCount, healthStatus)
    }

    suspend fun getEntryCountBySourceId(sourceId: Int): Int {
        return entryDao.getEntryCountBySourceId(sourceId)
    }

    fun getTotalEntryCount(): Flow<Int> {
        return entryDao.getTotalEntryCount()
    }

    fun getEnabledSourcesEntryCount(): Flow<Int> {
        return getEnabledSources().map { sources ->
            var total = 0
            sources.forEach { source ->
                total += source.entriesCount
            }
            total
        }
    }

    private fun normalizePhoneNumber(raw: String): String {
        if (raw.isBlank()) return ""
        var cleaned = raw.replace(Regex("[^0-9+\\s]"), "").trim()
        if (cleaned.startsWith("1") && cleaned.length == 11) {
            cleaned = "+$cleaned"
        } else if (!cleaned.startsWith("+")) {
            cleaned = "+1$cleaned"
        }
        return cleaned
    }

    suspend fun getCallDecision(rawNumber: String): CallDecision {
        val normalized = normalizePhoneNumber(rawNumber)
        if (normalized.isBlank()) {
            return CallDecision("ALLOW", "Invalid number", 0, "default")
        }

        entryDao.findUnifiedAllowEntry(normalized)?.let {
            return CallDecision("ALLOW", "Manual Allow List", it.confidence ?: 0, "manual_allow")
        }

        entryDao.findUnifiedBlockEntry(normalized)?.let {
            return CallDecision("BLOCK", "Manual Block List", it.confidence ?: 0, "manual_block")
        }

        val patterns = entryDao.getAllBlockPatterns()
        patterns.find { normalized.startsWith(it.phoneNumber) }?.let {
            return CallDecision("BLOCK", "Pattern: ${it.phoneNumber}", it.confidence ?: 0, "pattern")
        }

        entryDao.findEntriesByPhoneNumber(normalized).firstOrNull { it.action == "BLOCK" }?.let {
            return CallDecision("BLOCK", it.metadata ?: "External Source", it.confidence ?: 0, "aggregated")
        }

        return CallDecision("ALLOW", "No rule matched", 0, "default")
    }

    suspend fun getAllEntries(): List<UnifiedEntryEntity> {
        return entryDao.getAllEntries()
    }

    suspend fun insertEntry(entry: UnifiedEntryEntity) {
        val sanitized = entry.copy(phoneNumber = normalizePhoneNumber(entry.phoneNumber))
        entryDao.insertEntry(sanitized)
    }

    suspend fun deleteEntry(entry: UnifiedEntryEntity) {
        entryDao.deleteEntry(entry)
    }

    data class CallDecision(
        val action: String,
        val reason: String,
        val confidence: Int,
        val source: String
    )
}
