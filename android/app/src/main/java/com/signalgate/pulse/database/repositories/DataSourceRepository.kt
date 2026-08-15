package com.signalgate.pulse.database.repositories

import com.signalgate.pulse.data.security.BloomFilterEngine
import com.signalgate.pulse.data.security.SanitizationEngine
import com.signalgate.pulse.database.daos.SourceDao
import com.signalgate.pulse.database.daos.UnifiedEntryDao
import com.signalgate.pulse.database.entities.SourceEntity
import com.signalgate.pulse.database.entities.UnifiedEntryEntity
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
 *
 * Bloom fast-pass (this session):
 * getCallDecision() now checks two BloomFilterEngine instances before touching
 * Room at all — [bloomFilter] for exact phone numbers, [patternBloomFilter] for
 * block-pattern prefixes (see matchesAnyPatternPrefix()). Both are populated at
 * the same chokepoint as everything else in this class, insertEntry(), so every
 * manual rule, contacts import, and federal sync (CSV or FTC JSON API) keeps the
 * filters in sync with the DB automatically — no separate write path to forget.
 *
 * Bloom filters never produce false negatives, only false positives. That means:
 *   - A "not present" bloom result is a hard guarantee — safe to skip the DB read.
 *   - A "might be present" bloom result is NOT a decision — it only means "go
 *     check the real DB", exactly like today. The DB result is always authoritative;
 *     the bloom filters only ever skip reads, never skip or override a decision.
 *
 * Because BloomFilterEngine is an in-memory BitSet, it is empty on every fresh
 * process — see rehydrateBloomFilters() for how it gets refilled.
 *
 * Rehydration readiness (revised this session): rehydration runs on a
 * background coroutine kicked off from MainApplication, NOT inside
 * AppModule.initializeDatabase()'s runBlocking startup path. Unlike
 * DatabaseInitializer.seedRequiredSources() — which IS binding, because
 * getCallDecision() and BlocklistRepository structurally depend on the
 * MANUAL source row existing — the bloom filters are a pure read-skip
 * optimization. getCallDecision() is fully correct with empty filters, just
 * not yet fast, so there's no correctness reason to block startup (and, on a
 * cold CallScreeningService-triggered process spawn, block answering a real
 * incoming call) on a rebuild that scales with total row count, up to the
 * 500,000-element capacity BloomFilterEngine is sized for.
 *
 * [bloomReady] tracks this: false until rehydration completes, during which
 * getCallDecision() skips the fast-pass check entirely and queries Room
 * directly — identical to how this class behaved before bloom filters
 * existed. Never incorrect, just not optimized yet.
 */
class DataSourceRepository(
    private val sourceDao: SourceDao,
    private val entryDao: UnifiedEntryDao,
    private val bloomFilter: BloomFilterEngine,
    private val patternBloomFilter: BloomFilterEngine
) {
    companion object {
        /**
         * Phase 0.3 (Security Control-Plane Integrity — source lifecycle
         * semantics, Architecture Contract §7 / INV-008): source *types* that
         * may never be deleted via deleteSource(), only their entries.
         *
         * "MANUAL" covers BOTH seeded protected sources — DatabaseInitializer
         * seeds "Manual User Rules" (manual_source_id) and "Contacts Allow
         * List" (contacts_source_id) with the same type = "MANUAL", distinguished
         * only by name/pathOrUrl, not by a separate type value. Guarding on
         * type therefore protects both with one check, and keeps working even
         * if a source's numeric id changes (e.g. reseeded on a fresh install).
         *
         * "FTC" / "FCC" are federal sources (see ReliableSourceManager):
         * disableable via toggleSourceEnabled(), but never deletable.
         *
         * Deliberately NOT included: "CSV", "XLSX", "URL", or any other future
         * type — those identify user-created sources, which the contract
         * allows to be either disabled or deleted.
         */
        val PROTECTED_SOURCE_TYPES = setOf("MANUAL", "FTC", "FCC")
    }

    @Volatile
    private var bloomReady = false

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

    /**
     * Phase 0.3: refuses to delete a protected source (see
     * [PROTECTED_SOURCE_TYPES]). This is the actual chokepoint every current
     * caller already routes through (SourcesViewModel today; SecurityRuleRepository
     * for any future decision-affecting caller) — guarding here means the
     * refusal holds regardless of which layer calls it, rather than relying on
     * every future caller to remember to check first.
     *
     * A source deletion cascades (FK CASCADE) into every unified_entries row
     * and sync_history row for that source — see SourceDeletionCascadeTest.
     * For a protected source that cascade would silently erase every manual
     * rule, every contacts-derived allow entry, or an entire federal dataset,
     * with no independent confirmation step. Refusing before the DAO call is
     * reached is what makes that impossible rather than just unlikely.
     */
    suspend fun deleteSource(source: SourceEntity) {
        if (source.type in PROTECTED_SOURCE_TYPES) {
            throw ProtectedSourceDeletionException(source)
        }
        sourceDao.deleteSource(source)
    }

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
     *
     * Bloom fast-pass (this session): also inserts into the appropriate bloom
     * filter, chosen by isPattern — pattern rows (e.g. "+1900") go into
     * [patternBloomFilter] since they're a prefix fragment, not a real number,
     * and would never exact-match anything if inserted into [bloomFilter]. This
     * is the same chokepoint every entry type already funnels through, so both
     * filters stay comprehensive automatically as new rules arrive.
     */
    suspend fun insertEntry(entry: UnifiedEntryEntity) {
        val sanitized = entry.copy(
            phoneNumber = normalizePhoneNumber(entry.phoneNumber),
            category = entry.category?.let { SanitizationEngine.sanitizeTextField(it) },
            metadata = entry.metadata?.let { SanitizationEngine.sanitizeTextField(it) }
        )
        entryDao.insertEntry(sanitized)
        if (sanitized.isPattern) {
            patternBloomFilter.insert(sanitized.phoneNumber)
        } else {
            bloomFilter.insert(sanitized.phoneNumber)
        }
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
     *
     * Bloom fast-pass (this session): Step 1.5, below, runs before Step 2, but
     * only if [bloomReady] — see class doc for why rehydration is allowed to
     * still be in progress (or not yet started) at any given call, and why
     * that's safe. If neither bloom filter can possibly contain this number,
     * both Room reads (Step 2's exact lookup and Step 3's pattern query) are
     * skipped entirely and this returns the same "default" ALLOW that Step 4
     * would have produced anyway — this is a read-skip optimization, not a
     * new decision path.
     */
    suspend fun getCallDecision(rawNumber: String): CallDecision {
        val normalized = normalizePhoneNumber(rawNumber)
        if (normalized.isBlank()) {
            return CallDecision("ALLOW", "Invalid number", 0, "default")
        }

        // Step 1.5: bloom fast-pass. See class doc + matchesAnyPatternPrefix() for
        // why a negative result is a guarantee, not a guess — but only once
        // rehydration has actually finished. Not ready yet → behave exactly as
        // if bloom filters didn't exist (always fall through to Room below).
        val mightHaveExactEntry = !bloomReady || bloomFilter.mightContain(normalized)
        val mightMatchPattern = !bloomReady || matchesAnyPatternPrefix(normalized)
        if (!mightHaveExactEntry && !mightMatchPattern) {
            return CallDecision("ALLOW", "No rule matched (bloom fast-pass)", 0, "default")
        }

        // Step 2: exact-match lookup with priority ordering
        val exactMatches = if (mightHaveExactEntry) {
            entryDao.findEntriesByPhoneNumberWithPriority(normalized)
        } else {
            emptyList()
        }

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
        if (mightMatchPattern) {
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
        }

        return CallDecision("ALLOW", "No rule matched", 0, "default")
    }

    /**
     * Bloom-backed prefix check for pattern/prefix block rules (Step 3's guard).
     *
     * BloomFilterEngine only supports exact-membership tests, but block patterns
     * are matched by normalized.startsWith(pattern) — a prefix relation, not
     * equality. To use a bloom filter safely for this without introducing false
     * negatives, insertEntry() stores the *pattern string itself* (e.g. "+1900")
     * as a member of [patternBloomFilter] — not the numbers it would match. Here,
     * the check is flipped: every prefix of the incoming number is tested for
     * exact membership. If "+1900" was ever inserted as a pattern, the prefix
     * "+1900" of an incoming "+19005551234" call is an exact string match, and a
     * bloom filter cannot miss an exact match that was actually inserted — so a
     * "no prefix matched" result here is a hard guarantee that Step 3's DB query
     * would find nothing, not a probabilistic guess.
     *
     * The reverse isn't guaranteed: a hit may be a false positive on a prefix
     * that was never actually inserted (~2% by design). That's fine — a hit just
     * means "don't skip Step 3's DB query", exactly as if bloom weren't involved.
     *
     * Bounded to normalized.length iterations (a sanitized number, ≤ ~16 chars),
     * so worst case is ~16 cheap hash computations — far cheaper than the DB
     * query it's guarding.
     */
    private fun matchesAnyPatternPrefix(normalized: String): Boolean {
        if (normalized.isEmpty()) return false
        for (len in 1..normalized.length) {
            if (patternBloomFilter.mightContain(normalized.substring(0, len))) {
                return true
            }
        }
        return false
    }

    /**
     * Rehydrates both bloom filters from the DB.
     *
     * BloomFilterEngine is an in-memory BitSet — it comes back empty on every
     * fresh process, including a cold start triggered directly by a
     * CallScreeningService callback before any Activity exists.
     *
     * Runs off the startup-blocking path (see class doc) — called from a
     * background coroutine launched in MainApplication, after
     * AppModule.initializeDatabase() completes, not inside it. [bloomReady]
     * is set false for the ENTIRE clear+rebuild window, not just before it
     * starts: between clear() and the last insert(), the filters read as
     * "nothing present," which is only a safe signal to trust once the
     * rebuild is actually done. getCallDecision() checks [bloomReady] before
     * trusting either filter, so a call arriving mid-rehydration just queries
     * Room directly — never an incorrect skip.
     *
     * Safe to call more than once (e.g. a defensive re-call) without
     * double-inserting, since both filters are cleared first.
     */
    suspend fun rehydrateBloomFilters() {
        bloomReady = false
        bloomFilter.clear()
        patternBloomFilter.clear()
        for (entry in entryDao.getAllEntries()) {
            if (entry.isPattern) {
                patternBloomFilter.insert(entry.phoneNumber)
            } else {
                bloomFilter.insert(entry.phoneNumber)
            }
        }
        bloomReady = true
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

/**
 * Thrown by [DataSourceRepository.deleteSource] when the target source's type
 * is in [DataSourceRepository.PROTECTED_SOURCE_TYPES]. Per Architecture
 * Contract §7 / INV-008, MANUAL, CONTACTS, and federal (FTC/FCC) sources may
 * never be deleted — only their entries, or (for federal sources) disabled
 * via toggleSourceEnabled(). Callers should treat this the same as any other
 * rejected mutation, not as an unexpected failure: SourcesViewModel already
 * catches Exception around this call site and logs it rather than crashing.
 */
class ProtectedSourceDeletionException(source: SourceEntity) : IllegalStateException(
    "Source '${source.name}' (id=${source.id}, type=${source.type}) is protected and " +
        "cannot be deleted. MANUAL/CONTACTS sources may never be deleted (only their " +
        "entries); federal sources (FTC/FCC) may be disabled but not deleted. " +
        "See Architecture Contract §7, INV-008."
)
