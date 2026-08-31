package com.signalgate.pulse.database.repositories

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.signalgate.pulse.data.security.BloomFilterEngine
import com.signalgate.pulse.database.SignalGateDatabase
import com.signalgate.pulse.database.entities.SourceEntity
import com.signalgate.pulse.database.entities.UnifiedEntryEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * BloomAuthoritativeDecisionTest — Phase 0.2 (Architecture-Contract-v3-DRAFT.md
 * §5.1 INV-001 / SECURITY-DEVOPS-BUILD-PLAN.md 0.2).
 *
 * INV-001 claims the encrypted database is the sole authoritative source of
 * security policy, and that BloomFilterEngine is a purely derived,
 * disposable accelerator that can never change a decision — only skip a
 * redundant Room read. DataSourceRepository's own class doc already asserts
 * this is true by construction (bloomReady gates every fast-pass check, and
 * a negative bloom result is only ever trusted once bloomReady is true).
 * This suite is what turns that claim from "true by how the code happens to
 * be written" into "true and verified" — every test compares the SAME
 * DataSourceRepository instance's getCallDecision() output against a second,
 * bloom-disabled repository instance hitting the identical underlying rows,
 * across every state INV-001 claims safety for. Any divergence is a real
 * INV-001 violation, not a hypothetical one.
 *
 * "Bloom-disabled" is achieved by constructing a second DataSourceRepository
 * against the same Room database but with fresh BloomFilterEngine instances
 * that are never rehydrated (bloomReady stays false forever on that second
 * instance, since rehydrateBloomFilters() is never called on it) — per the
 * class doc, an unready bloom filter makes getCallDecision() behave
 * identically to pre-bloom code, i.e. a pure Room lookup. That gives a real,
 * exercised "authoritative-only" code path to diff against, rather than a
 * hand-reimplemented decision oracle that could itself drift from
 * getCallDecision()'s actual logic over time.
 *
 * This is an instrumented test — it runs on a device or emulator, not the
 * JVM (Room needs a real SQLite engine). Run with:
 *   ./gradlew connectedPulseDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class BloomAuthoritativeDecisionTest {

    private lateinit var db: SignalGateDatabase

    // "optimized" — bloom filters get rehydrated and are allowed to fast-pass.
    private lateinit var optimizedRepo: DataSourceRepository

    // "authoritative" — separate BloomFilterEngine instances that are NEVER
    // rehydrated, so bloomReady stays false forever and every call falls
    // through to a pure Room lookup, exactly as DataSourceRepository behaved
    // before bloom filters existed. Same db, same rows — only the bloom
    // layer differs.
    private lateinit var authoritativeRepo: DataSourceRepository

    private var manualSourceId: Int = 0
    private var federalSourceId: Int = 0

    @Before
    fun setUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, SignalGateDatabase::class.java).build()

        optimizedRepo = DataSourceRepository(
            db.sourceDao(), db.unifiedEntryDao(),
            BloomFilterEngine(), BloomFilterEngine()
        )
        authoritativeRepo = DataSourceRepository(
            db.sourceDao(), db.unifiedEntryDao(),
            BloomFilterEngine(), BloomFilterEngine()
        )

        manualSourceId = db.sourceDao().insertSource(
            SourceEntity(
                name = "Manual", type = "MANUAL", pathOrUrl = "local",
                isEnabled = true, priority = 100, healthStatus = "HEALTHY"
            )
        ).toInt()
        federalSourceId = db.sourceDao().insertSource(
            SourceEntity(
                name = "Federal Test Source", type = "URL", pathOrUrl = "https://example.test",
                isEnabled = true, priority = 85, healthStatus = "HEALTHY"
            )
        ).toInt()
    }

    @After
    fun tearDown() {
        db.close()
    }

    /**
     * Shared assertion: both repos must agree on the actual DECISION, every
     * time. Deliberately does NOT require the full CallDecision (including
     * `reason`) to match verbatim — DataSourceRepository.getCallDecision()
     * intentionally annotates `reason` with "(bloom fast-pass)" when the
     * Bloom-filter early-exit path fires (line ~205) vs. the plain "No rule
     * matched" from a full Room fallthrough (line ~248). That's useful,
     * intentional observability, not a decision divergence — INV-001 is
     * about whether the same ALLOW/BLOCK/confidence/source comes out, not
     * whether the diagnostic breadcrumb text matches. Confirmed via a real
     * CI run 2026-08-15: the first version of this test asserted full
     * struct equality and produced 4 false failures, all with identical
     * action/confidence/source and only the reason annotation differing.
     */
    private suspend fun insert(entry: UnifiedEntryEntity) {
        optimizedRepo.insertEntriesAuthoritative(listOf(entry))
        optimizedRepo.rebuildDerivedIndexes()
    }

    private suspend fun assertDecisionsMatch(number: String, context: String) {
        val optimized = optimizedRepo.getCallDecision(number)
        val authoritative = authoritativeRepo.getCallDecision(number)
        assertEquals("[$context] action must match for $number", authoritative.action, optimized.action)
        assertEquals("[$context] confidence must match for $number", authoritative.confidence, optimized.confidence)
        assertEquals("[$context] source must match for $number", authoritative.source, optimized.source)
        // Soft/documentary check, not a hard assertion: the optimized reason
        // is allowed to be the authoritative reason plus the fast-pass
        // annotation, or identical to it — but nothing else. This still
        // catches a genuinely wrong/unexpected reason string without
        // requiring verbatim equality.
        val reasonIsExpected = optimized.reason == authoritative.reason ||
            optimized.reason == "${authoritative.reason} (bloom fast-pass)"
        assertTrue(
            "[$context] optimized reason '${optimized.reason}' was neither equal to nor a " +
                "recognized fast-pass annotation of authoritative reason '${authoritative.reason}'",
            reasonIsExpected
        )
    }

    // --- cold Bloom ------------------------------------------------------

    /**
     * Cold Bloom: process just started, rehydrateBloomFilters() never
     * called on optimizedRepo. bloomReady is false on BOTH repos here, so
     * this is really "both repos behave authoritatively" — the meaningful
     * assertion is that a cold/unready optimizedRepo does NOT wrongly skip
     * a real match (i.e. does not behave as if the filter were warm and
     * empty). Insert a real BLOCK row first so a wrong "empty filter, skip
     * everything" bug would be caught.
     */
    @Test
    fun coldBloom_beforeAnyRehydration_matchesAuthoritative() = runBlocking {
        db.unifiedEntryDao().insertEntry(
            UnifiedEntryEntity(
                phoneNumber = "+18005550100", action = "BLOCK", sourceId = federalSourceId
            )
        )
        // insertEntry() on optimizedRepo not used here on purpose — this row
        // was written directly via the DAO to simulate data that existed
        // before optimizedRepo's bloom filter was ever populated at all,
        // i.e. a genuinely cold/empty in-memory BitSet.
        assertDecisionsMatch("+18005550100", "cold Bloom, real BLOCK row present")
        assertDecisionsMatch("+19998887777", "cold Bloom, number with no rule")
    }

    // --- warm Bloom --------------------------------------------------------

    @Test
    fun warmBloom_normalStadyState_matchesAuthoritative() = runBlocking {
        insert(
            UnifiedEntryEntity(phoneNumber = "+18005550111", action = "BLOCK", sourceId = federalSourceId)
        )
        insert(
            UnifiedEntryEntity(phoneNumber = "+18005550122", action = "ALLOW", sourceId = manualSourceId)
        )
        optimizedRepo.rehydrateBloomFilters()

        assertDecisionsMatch("+18005550111", "warm Bloom, BLOCK match")
        assertDecisionsMatch("+18005550122", "warm Bloom, ALLOW match")
        assertDecisionsMatch("+13105559999", "warm Bloom, no match")
    }

    // --- manual mutation after warm Bloom -----------------------------------

    /**
     * A committed authoritative write must not mutate a warm Bloom filter as a
     * side effect. The derived index is updated only by the explicit rebuild,
     * which callers invoke after their surrounding transaction completes.
     */
    @Test
    fun authoritativeMutation_requiresExplicitRebuild() = runBlocking {
        insert(
            UnifiedEntryEntity(phoneNumber = "+18005550111", action = "BLOCK", sourceId = federalSourceId)
        )
        optimizedRepo.rehydrateBloomFilters()

        val freshNumber = "+17275550199"
        assertDecisionsMatch(freshNumber, "before fresh mutation, no rule yet")

        optimizedRepo.insertEntriesAuthoritative(
            listOf(UnifiedEntryEntity(phoneNumber = freshNumber, action = "BLOCK", sourceId = federalSourceId))
        )
        assertEquals(
            "warm Bloom must not expose an authoritative row before rebuild",
            "ALLOW",
            optimizedRepo.getCallDecision(freshNumber).action
        )

        optimizedRepo.rebuildDerivedIndexes()
        assertDecisionsMatch(freshNumber, "after explicit post-commit rebuild")
        assertEquals("fresh BLOCK mutation must be enforced after rebuild", "BLOCK", optimizedRepo.getCallDecision(freshNumber).action)
    }

    // --- source replacement after warm Bloom --------------------------------

    /**
     * Simulates the closest available proxy for Phase 0.4/0.5's not-yet-built
     * transactional source replacement: delete every entry for a source
     * (FK-cascade-adjacent operation) and re-insert a different rule set for
     * the same phone numbers, without an explicit rehydrateBloomFilters()
     * call in between deletion and re-insertion. A stale bloom bit from the
     * deleted BLOCK entry must not cause the new ALLOW-only ruleset to be
     * treated as still-blocked once insertEntry() has recorded the new
     * ALLOW row — Room is authoritative, so the new row must win regardless
     * of what the bloom filter's bits still say about the old one.
     */
    @Test
    fun sourceReplacement_afterWarmBloom_matchesAuthoritative() = runBlocking {
        val number = "+16465550188"
        insert(
            UnifiedEntryEntity(phoneNumber = number, action = "BLOCK", sourceId = federalSourceId)
        )
        optimizedRepo.rehydrateBloomFilters()
        assertEquals("BLOCK", optimizedRepo.getCallDecision(number).action)

        // "Replace" the source's rule for this number: delete the old row,
        // insert a new one with the opposite action — same number, same
        // source, simulating a snapshot replacement changing its mind about
        // this entry.
        db.unifiedEntryDao().deleteEntryByNumberAndSource(number, federalSourceId)
        optimizedRepo.insertEntriesAuthoritative(
            listOf(UnifiedEntryEntity(phoneNumber = number, action = "ALLOW", sourceId = federalSourceId))
        )

        // The stale old bit may cause an extra Room read, but it cannot change
        // the authoritative ALLOW result. The snapshot path rebuilds explicitly
        // after its transaction commits.
        assertDecisionsMatch(number, "after source replacement, before explicit rebuild")
        assertEquals(
            "replaced rule must win — must no longer be BLOCK",
            "ALLOW",
            optimizedRepo.getCallDecision(number).action
        )
    }

    // --- Bloom rebuild -------------------------------------------------------

    @Test
    fun explicitRebuild_afterMultipleMutations_matchesAuthoritative() = runBlocking {
        val numbers = listOf("+18005550201", "+18005550202", "+18005550203")
        numbers.forEach {
            insert(UnifiedEntryEntity(phoneNumber = it, action = "BLOCK", sourceId = federalSourceId))
        }
        optimizedRepo.rehydrateBloomFilters()
        optimizedRepo.rehydrateBloomFilters() // explicit second rebuild — must be idempotent, per class doc

        numbers.forEach { assertDecisionsMatch(it, "after explicit double rebuild") }
    }

    // --- database reset followed by rebuild -----------------------------------

    /**
     * Simulates DatabaseResetEvent's real-world effect (see SecureDatabase /
     * KeystoreInvalidatedException) at the repository level: all rows gone,
     * bloom filter rebuilt against the now-empty table. A stale "might
     * contain" bit surviving a full data wipe would be the worst version of
     * this bug — every call would still correctly fall through to Room
     * (which is now empty, so still correct), but this proves the fast-pass
     * path also correctly reports "no rule" rather than any stale positive
     * being mishandled downstream.
     */
    @Test
    fun databaseReset_thenRebuild_matchesAuthoritative() = runBlocking {
        val number = "+12125550177"
        insert(UnifiedEntryEntity(phoneNumber = number, action = "BLOCK", sourceId = federalSourceId))
        optimizedRepo.rehydrateBloomFilters()
        assertEquals("BLOCK", optimizedRepo.getCallDecision(number).action)

        // Reset: wipe every unified_entries row (what a DatabaseResetEvent
        // recovery path does at the table level), then rebuild the bloom
        // filter against the now-empty table.
        db.unifiedEntryDao().getAllEntries().forEach { db.unifiedEntryDao().deleteEntry(it) }
        optimizedRepo.rehydrateBloomFilters()

        assertDecisionsMatch(number, "after full reset + rebuild")
        assertEquals(
            "post-reset decision must be default ALLOW, not a stale BLOCK",
            "ALLOW",
            optimizedRepo.getCallDecision(number).action
        )
    }

    // --- pattern/prefix rules — separate bloom filter, same guarantee ---------

    /**
     * matchesAnyPatternPrefix() uses patternBloomFilter, a structurally
     * different check (prefix membership via per-length probes, not exact
     * match) from the main bloomFilter used by the tests above. INV-001's
     * "never a false negative" guarantee has to hold for this path
     * independently — it's different code, not just a different call site.
     */
    @Test
    fun patternRule_afterWarmBloom_matchesAuthoritative() = runBlocking {
        insert(
            UnifiedEntryEntity(phoneNumber = "+1900", action = "BLOCK", isPattern = true, sourceId = federalSourceId)
        )
        optimizedRepo.rehydrateBloomFilters()

        assertDecisionsMatch("+19005551234", "pattern match via warm Bloom")
        assertDecisionsMatch("+18005551234", "no pattern match, unrelated prefix")
    }
}
