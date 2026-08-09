package com.signalgate.multipoint.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.signalgate.multipoint.database.entities.SourceEntity
import com.signalgate.multipoint.database.entities.SyncHistoryEntry
import com.signalgate.multipoint.database.entities.UnifiedEntryEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SourceDeletionCascadeTest — confirms the FK CASCADE declared on
 * UnifiedEntryEntity.sourceId and SyncHistoryEntry.sourceId (both pointing at
 * SourceEntity, onDelete = ForeignKey.CASCADE) actually fires at runtime.
 *
 * Context: a review raised the concern that deleting a source could leave
 * orphaned unified_entries / sync_history rows behind (stale blocklist/allow
 * entries still being matched against incoming calls after their source is
 * gone). The schema already declares CASCADE for both relationships — this
 * test exists because a declared FK constraint is only enforced if
 * PRAGMA foreign_keys = ON is active on the connection. Room enables this by
 * default when foreign keys are declared and no custom
 * SupportSQLiteOpenHelperFactory/Callback overrides it (neither exists in
 * SignalGateDatabase), but that's an inference from framework behavior, not
 * something confirmable by reading source — hence this test.
 *
 * Uses a fresh in-memory database per test (not the exported v1/v2 schema
 * fixtures MigrationTest uses) since this is checking constraint enforcement
 * against current entity definitions, not migration correctness.
 *
 * This is an instrumented test — it runs on a device or emulator, not the JVM.
 * Run with: ./gradlew connectedPulseDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class SourceDeletionCascadeTest {

    private lateinit var db: SignalGateDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, SignalGateDatabase::class.java)
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    /**
     * Deleting a source must cascade-delete every unified_entries row that
     * points at it. If this fails, FK enforcement is not active and the
     * dead deleteEntriesBySourceId() DAO method should be wired back in as
     * an explicit fallback rather than left unused.
     */
    @Test
    fun deletingSource_cascadesUnifiedEntries() = runBlocking {
        val sourceDao = db.sourceDao()
        val entryDao = db.unifiedEntryDao()

        val sourceId = sourceDao.insertSource(
            SourceEntity(
                name = "Cascade Test Source",
                type = "MANUAL",
                pathOrUrl = "local",
                isEnabled = true,
                priority = 50,
                healthStatus = "HEALTHY"
            )
        ).toInt()

        entryDao.insertEntry(
            UnifiedEntryEntity(
                phoneNumber = "+18005551234",
                action = "BLOCK",
                sourceId = sourceId
            )
        )

        assertEquals(
            "sanity check — entry must exist before deletion",
            1,
            entryDao.getEntryCountBySourceId(sourceId)
        )

        val source = sourceDao.getSourceById(sourceId)
        assertTrue("source must exist before deletion", source != null)
        sourceDao.deleteSource(source!!)

        assertEquals(
            "unified_entries row must be cascade-deleted when its source is removed",
            0,
            entryDao.getEntryCountBySourceId(sourceId)
        )
    }

    /**
     * Same check for sync_history, which carries the identical
     * onDelete = ForeignKey.CASCADE relationship to SourceEntity.
     */
    @Test
    fun deletingSource_cascadesSyncHistory() = runBlocking {
        val sourceDao = db.sourceDao()
        val syncHistoryDao = db.syncHistoryDao()

        val sourceId = sourceDao.insertSource(
            SourceEntity(
                name = "Cascade Test Source 2",
                type = "CSV",
                pathOrUrl = "local2",
                isEnabled = true,
                priority = 40,
                healthStatus = "HEALTHY"
            )
        ).toInt()

        syncHistoryDao.insertSyncHistory(
            SyncHistoryEntry(
                sourceId = sourceId,
                status = "SUCCESS",
                entriesAdded = 3
            )
        )

        assertEquals(
            "sanity check — sync history row must exist before deletion",
            1,
            syncHistoryDao.getSyncHistoryBySourceId(sourceId).size
        )

        val source = sourceDao.getSourceById(sourceId)
        assertTrue("source must exist before deletion", source != null)
        sourceDao.deleteSource(source!!)

        assertEquals(
            "sync_history row must be cascade-deleted when its source is removed",
            0,
            syncHistoryDao.getSyncHistoryBySourceId(sourceId).size
        )
    }

    /**
     * Negative check — deleting one source must not affect entries
     * belonging to a different, still-existing source. Guards against a
     * cascade misconfiguration deleting more broadly than intended.
     */
    @Test
    fun deletingSource_doesNotAffectOtherSourcesEntries() = runBlocking {
        val sourceDao = db.sourceDao()
        val entryDao = db.unifiedEntryDao()

        val sourceToDeleteId = sourceDao.insertSource(
            SourceEntity(
                name = "Delete Me",
                type = "MANUAL",
                pathOrUrl = "local",
                isEnabled = true,
                priority = 50,
                healthStatus = "HEALTHY"
            )
        ).toInt()

        val sourceToKeepId = sourceDao.insertSource(
            SourceEntity(
                name = "Keep Me",
                type = "MANUAL",
                pathOrUrl = "local",
                isEnabled = true,
                priority = 60,
                healthStatus = "HEALTHY"
            )
        ).toInt()

        entryDao.insertEntry(
            UnifiedEntryEntity(
                phoneNumber = "+18005551234",
                action = "BLOCK",
                sourceId = sourceToDeleteId
            )
        )
        entryDao.insertEntry(
            UnifiedEntryEntity(
                phoneNumber = "+18005556789",
                action = "ALLOW",
                sourceId = sourceToKeepId
            )
        )

        val sourceToDelete = sourceDao.getSourceById(sourceToDeleteId)
        assertTrue(sourceToDelete != null)
        sourceDao.deleteSource(sourceToDelete!!)

        assertEquals(
            "deleted source's entry must be gone",
            0,
            entryDao.getEntryCountBySourceId(sourceToDeleteId)
        )
        assertEquals(
            "other source's entry must be untouched",
            1,
            entryDao.getEntryCountBySourceId(sourceToKeepId)
        )
    }
}
