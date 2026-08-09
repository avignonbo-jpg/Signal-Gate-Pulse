package com.signalgate.multipoint.database.repositories

import com.signalgate.multipoint.data.security.SanitizationEngine
import com.signalgate.multipoint.database.daos.SourceDao
import com.signalgate.multipoint.database.daos.UnifiedEntryDao
import com.signalgate.multipoint.database.entities.SourceEntity
import com.signalgate.multipoint.database.entities.UnifiedEntryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataSourceRepository — single source of truth for data source and entry operations.
 *
 * Phase 2.6 — Source conflict resolution:
 * getCallDecision() now uses priority-ordered DAO queries (join with sources table)
 * so that when multiple sources disagree on a number, the highest-priority source
 * wins. MANUAL source is seeded at priority 100 by DatabaseInitializer — user
 * decisions always beat external sources regardless of source type.
 *
 * Conflict resolution hierarchy (highest priority wins at each tier):
 *   Tier 1: ALLOW entries in priority order — if highest-priority source ALLOWs, allow.
 *   Tier 2: BLOCK entries in priority order — first BLOCK after any ALLOW check.
 *   Tier 3: Pattern/prefix entries in priority order.
 *   Default: ALLOW (no rule matched).
 *
 * The MANUAL source (priority 100) always outranks federal sources (priority 85–90),
 * so a user's explicit "not spam" overturn is never overridden by a federal list.
 */
class DataSourceRepository(
    private val sourceDao: SourceDao,
    private val entryDao: UnifiedEntryDao
) {

    fun getAllSources(): Flow<List<SourceEntity>> = sourceDao.getAllSources()

    fun getEnabledSources(): Flow<List<SourceEntity>> = sourceDao.getEnabledSources()

    fun getSourceCount(): Flow<Int> = sourceDao.getSourceCount()

    fun getEnabledSourceCount(): Flow<Int> =
        getAllSources().map { sources -> sources.count { it.isEnabled } }

    suspend fun getSourceById(id: Int): SourceEntity? = sourceDao.getSourceById(id)

    /**
     * Looks up a source by exact name. One-shot suspend query — never use
     * getAllSources().collect{} for this; that Flow never completes.
     */
    suspend fun getSourceByName(name: String): SourceEntity? = sourceDao.getSourceByName(name)

    suspend fun insertSource(source: SourceEntity): Long = sourceDao.insertSource(source)

    suspend fun updateSource(source: SourceEntity) = sourceDao.updateSource(source)

    suspend fun deleteSource(source: SourceEntity) = sourceDao.deleteSource(source)

    suspend fun toggleSourceEnabled(sourceId: Int, isEnabled: Boolean) =
        sourceDao.updateSourceEnabled(sourceId, isEnabled)

    suspend fun updateSourceSyncStatus(
        sourceId: Int,
        timestamp: Long,
        entriesCount: Int,
        healthStatus: String
    ) = sourceDao.updateSourceSyncStatus(sourceId, timestamp, entriesCount, healthStatus)

    suspend fun getEntryCountBySourceId(sourceId: Int): Int =
        entryDao.getEntryCountBySourceId(sourceId)

    fun getTotalEntryCount(): Flow<Int> = entryDao.getTotalEntryCount()

    fun getEnabledSourcesEntryCount(): Flow<Int> =
        getEnabledSources().map { sources -> sources.sumOf { it.entriesCount } }

    suspend fun getAllEntries(): List<UnifiedEntryEntity> = entryDao.getAllEntries()

    /**
     * Security fix (audit finding): this is the single write chokepoint every
     * UnifiedEntryEntity insert funnels through (federal sync, CSV/XLSX import,
     * contacts allowlist, manual block/allow via RecentCallsViewModel). It must
     * never trust the caller to have sanitized first — normalizePhoneNumber()
     * now routes through SanitizationEngine.sanitizePhoneNumber() (previously a
     * private duplicate regex that never touched SanitizationEngine at all), and
     * category/metadata — the only free-text fields on this entity, and the ones
     * that can carry externally-sourced text such as a Contacts-provider display
     * name — are run through SanitizationEngine.sanitizeTextField().
     *
     * sanitizeTextField() is NOT idempotent (its SQL quote-escaping doubles up
     * on repeated application), so it must be applied exactly once. No current
     * caller pre-sanitizes category/metadata before calling insertEntry(), so
     * sanitizing here — once, centrally — is safe. Do not also sanitize these
     * fields at call sites that go through this method.
     */
    suspend fun insertEntry(entry: UnifiedEntryEntity) {
        val sanitized = entry.copy(
            phoneNumber = normalizePhoneNumber(entry.phoneNumber),
            category = entry.category?.let { SanitizationEngine.sanitizeTextField(it) },
            metadata = entry.metadata?.let { SanitizationEngine.sanitizeTextField(it) }
        )
        entryDao.insertEntry(sanitized)
    }

    suspend fun deleteEntry(entry: UnifiedEntryEntity) = entryDao.deleteEntry(entry)

    /**
     * Phase 2.6 — Priority-respecting call decision.
     *
     * Decision steps in order:
     *
     * 1. Normalize the number. Empty after normalization → default ALLOW.
     *
     * 2. Fetch all entries for this number joined with source priority, ordered
     *    highest priority first (findEntriesByPhoneNumberWithPriority). Walk the
     *    list in order:
     *    — First ALLOW entry wins immediately (highest-priority ALLOW).
     *    — First BLOCK entry wins if no ALLOW entry outranks it.
     *    This correctly handles: user marks not-spam (MANUAL priority 100 ALLOW)
     *    while federal list has it BLOCK (priority 85) → ALLOW wins.
     *
     * 3. Pattern/prefix check with priority ordering. Only reached if step 2
     *    found no exact match. Highest-priority pattern wins.
     *
     * 4. Default: ALLOW.
     *
     * Source labels in CallDecision.source:
     *   "manual_allow"  → Tier 1 ALLOWLISTED in CallScreeningEngine
     *   "manual_block"  → Tier 2 FEDERAL_BLOCK
     *   "aggregated"    → Tier 2 FEDERAL_BLOCK (external source)
     *   "pattern"       → Tier 3/4 depending on confidence in CallScreeningEngine
     *   "default"       → Tier 5 CLEAN_UNKNOWN
     */
    suspend fun getCallDecision(rawNumber: String): CallDecision {
        val normalized = normalizePhoneNumber(rawNumber)
        if (normalized.isBlank()) {
            return CallDecision("ALLOW", "Invalid number", 0, "default")
        }

        // Step 2: exact-match lookup with priority ordering
        val exactMatches = entryDao.findEntriesByPhoneNumberWithPriority(normalized)

        // Walk priority-ordered list — first entry determines the decision
        for (entry in exactMatches) {
            return when (entry.action) {
                "ALLOW" -> CallDecision(
                    action = "ALLOW",
                    reason = "Allow list (source priority ${entry.sourceId})",
                    confidence = entry.confidence ?: 100,
                    source = "manual_allow"
                )
                "BLOCK" -> CallDecision(
                    action = "BLOCK",
                    reason = entry.metadata ?: "Block list",
                    confidence = entry.confidence ?: 85,
                    source = if (isManualSource(entry.sourceId)) "manual_block" else "aggregated"
                )
                else -> continue
            }
        }

        // Step 3: pattern/prefix check with priority ordering
        val patterns = entryDao.getAllBlockPatternsWithPriority()
        val matchedPattern = patterns.firstOrNull { normalized.startsWith(it.phoneNumber) }
        if (matchedPattern != null) {
            return CallDecision(
                action = "BLOCK",
                reason = "Pattern: ${matchedPattern.phoneNumber}",
                confidence = matchedPattern.confidence ?: 85,
                source = "pattern"
            )
        }

        return CallDecision("ALLOW", "No rule matched", 0, "default")
    }

    /**
     * Determines if a sourceId belongs to a MANUAL source (user-created rules).
     * MANUAL sources have sourceId stored in SettingEntry under "manual_source_id".
     * For the decision label, we use a heuristic: MANUAL sources are seeded at
     * priority 100 by DatabaseInitializer. A source with priority 100 is MANUAL.
     * This avoids an extra DAO call per decision on the hot screening path.
     *
     * If the source can't be resolved, we conservatively label it "aggregated"
     * (external source) rather than "manual_block" — this means the engine will
     * still block but won't apply the FEDERAL_BLOCK tier treatment.
     */
    private suspend fun isManualSource(sourceId: Int): Boolean {
        return sourceDao.getSourceById(sourceId)?.priority == 100
    }

    /**
     * Security fix (audit finding): previously stripped characters with a private
     * `[^0-9+\s]` regex and never called SanitizationEngine — a second, divergent
     * sanitizer sitting right next to the canonical one. Now the raw input is
     * always passed through SanitizationEngine.sanitizePhoneNumber() first (strips
     * anything outside the audited allowlist and enforces the 30-char cap), and
     * the E.164 shaping below operates only on that already-sanitized string.
     */
    private fun normalizePhoneNumber(raw: String): String {
        val safe = SanitizationEngine.sanitizePhoneNumber(raw)
        if (safe.isBlank()) return ""
        var cleaned = safe.replace(Regex("[^0-9+]"), "").trim()
        if (cleaned.startsWith("1") && cleaned.length == 11) {
            cleaned = "+$cleaned"
        } else if (!cleaned.startsWith("+")) {
            cleaned = "+1$cleaned"
        }
        return cleaned
    }

    data class CallDecision(
        val action: String,
        val reason: String,
        val confidence: Int,
        val source: String
    )
}
