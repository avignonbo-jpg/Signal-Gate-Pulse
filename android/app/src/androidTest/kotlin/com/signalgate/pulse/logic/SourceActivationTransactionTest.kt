package com.signalgate.pulse.logic

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.signalgate.pulse.data.security.BloomFilterEngine
import com.signalgate.pulse.data.security.SecureCsvParser
import com.signalgate.pulse.database.SignalGateDatabase
import com.signalgate.pulse.database.entities.SourceEntity
import com.signalgate.pulse.database.entities.UnifiedEntryEntity
import com.signalgate.pulse.database.repositories.DataSourceRepository
import com.signalgate.pulse.database.repositories.SettingRepository
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 0.4 / INV-002 regression coverage.
 *
 * A candidate replacement that fails after deleting the old rows must leave the
 * previous active snapshot intact. The attempt timestamp is retained, while
 * the accepted timestamp remains unchanged.
 */
@RunWith(AndroidJUnit4::class)
class SourceActivationTransactionTest {

    private lateinit var database: SignalGateDatabase
    private lateinit var repository: SecurityRuleRepository
    private lateinit var dataSourceRepository: DataSourceRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, SignalGateDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dataSourceRepository = DataSourceRepository(
            database.sourceDao(),
            database.unifiedEntryDao(),
            BloomFilterEngine(),
            BloomFilterEngine()
        )
        repository = SecurityRuleRepository(
            dataSourceRepository,
            database,
            database.unifiedEntryDao(),
            SettingRepository(database.settingDao())
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun failedReplacement_preservesLastKnownGoodAndRecordsAttempt() = runBlocking {
        val insertedId = database.sourceDao().insertSource(
            SourceEntity(name = "Test Federal", type = "FTC", pathOrUrl = "test")
        )
        assertTrue("Source insert must return a valid row ID", insertedId > 0)
        val sourceId = insertedId.toInt()
        val oldEntry = UnifiedEntryEntity(
            phoneNumber = "+15550000001",
            action = "BLOCK",
            sourceId = sourceId
        )
        database.unifiedEntryDao().insertEntry(oldEntry)
        dataSourceRepository.rehydrateBloomFilters()

        val result = repository.replaceSourceSnapshot(
            sourceId,
            listOf(
                UnifiedEntryEntity(
                    phoneNumber = "+15550000002",
                    action = "BLOCK",
                    sourceId = 999999
                )
            )
        )

        assertTrue("replacement must report failure", result is SnapshotActivationResult.Failed)
        val persistedSource = database.sourceDao().getSourceById(sourceId)!!
        assertNotNull("attempt timestamp must be recorded", persistedSource.lastAttemptedSync)
        assertNull("failed replacement must not be accepted", persistedSource.lastAcceptedSnapshot)
        assertEquals("failed replacement must be FAILED", SourceLifecycleState.FAILED.name, persistedSource.lifecycleState)
        assertNotNull("failure reason must be recorded", persistedSource.lifecycleReason)
        assertTrue(
            "last-known-good entry must remain active",
            database.unifiedEntryDao().findEntriesBySourceId(sourceId).any { it.phoneNumber == oldEntry.phoneNumber }
        )
        assertEquals(
            "failed replacement must preserve the prior authoritative BLOCK decision",
            "BLOCK",
            dataSourceRepository.getCallDecision(oldEntry.phoneNumber).action
        )
        assertEquals(
            "failed candidate must not become a decision-relevant Bloom false positive",
            "ALLOW",
            dataSourceRepository.getCallDecision("+15550000002").action
        )
    }

    @Test
    fun acceptedReplacement_persistsLifecycleMetadata() = runBlocking {
        val sourceId = database.sourceDao().insertSource(
            SourceEntity(name = "Accepted Federal", type = "FTC", pathOrUrl = "test")
        ).toInt()
        val entry = UnifiedEntryEntity(
            phoneNumber = "+15550000003",
            action = "BLOCK",
            sourceId = sourceId
        )

        val result = repository.replaceSourceSnapshot(
            sourceId = sourceId,
            entries = listOf(entry),
            metadata = SnapshotMetadata(
                version = "2026-08-19T20:00:00Z",
                hash = "a".repeat(64),
                acceptedRecordCount = 1
            )
        )

        assertTrue("replacement must be accepted", result is SnapshotActivationResult.Accepted)
        val persistedSource = database.sourceDao().getSourceById(sourceId)!!
        assertEquals("accepted replacement must be HEALTHY", SourceLifecycleState.HEALTHY.name, persistedSource.lifecycleState)
        assertEquals("snapshot version must persist", "2026-08-19T20:00:00Z", persistedSource.snapshotVersion)
        assertEquals("snapshot hash must persist", "a".repeat(64), persistedSource.snapshotHash)
        assertEquals("accepted count must persist", 1, persistedSource.acceptedRecordCount)
        assertNotNull("accepted timestamp must be recorded", persistedSource.lastAcceptedSnapshot)
        assertNotNull("attempt timestamp must be recorded", persistedSource.lastAttemptedSync)
    }

    @Test
    fun emptyCandidate_isRejectedAndPreservesLastKnownGood() = runBlocking {
        val sourceId = database.sourceDao().insertSource(
            SourceEntity(name = "Empty Candidate Federal", type = "FTC", pathOrUrl = "test")
        ).toInt()
        val oldEntry = UnifiedEntryEntity(
            phoneNumber = "+15550000004",
            action = "BLOCK",
            sourceId = sourceId
        )
        database.unifiedEntryDao().insertEntry(oldEntry)

        val result = repository.replaceSourceSnapshot(sourceId, emptyList())

        assertTrue("empty candidate must be rejected", result is SnapshotActivationResult.Failed)
        val persistedSource = database.sourceDao().getSourceById(sourceId)!!
        assertEquals("empty candidate must be REJECTED", SourceLifecycleState.REJECTED.name, persistedSource.lifecycleState)
        assertNull("rejected candidate must not be accepted", persistedSource.lastAcceptedSnapshot)
        assertTrue(
            "empty candidate must not replace last-known-good entry",
            database.unifiedEntryDao().findEntriesBySourceId(sourceId).any { it.phoneNumber == oldEntry.phoneNumber }
        )
    }

    @Test
    fun batchedReplacement_rollsBackAllBatchesWhenProducerFails() = runBlocking {
        val sourceId = database.sourceDao().insertSource(
            SourceEntity(name = "Batched Federal", type = "FTC", pathOrUrl = "test")
        ).toInt()
        val oldEntry = UnifiedEntryEntity(
            phoneNumber = "+15550000005",
            action = "BLOCK",
            sourceId = sourceId
        )
        database.unifiedEntryDao().insertEntry(oldEntry)

        val result = repository.replaceSourceSnapshotBatched(sourceId) { emitBatch ->
            emitBatch(
                listOf(
                    UnifiedEntryEntity(
                        phoneNumber = "+15550000006",
                        action = "BLOCK",
                        sourceId = sourceId
                    )
                )
            )
            throw IllegalStateException("producer failed after first batch")
        }

        assertTrue("batched replacement must report failure", result is SnapshotActivationResult.Failed)
        val sourceEntries = database.unifiedEntryDao().findEntriesBySourceId(sourceId)
        assertTrue("failed batch must preserve last-known-good entry", sourceEntries.any { it.phoneNumber == oldEntry.phoneNumber })
        assertTrue("failed batch must not leave candidate entries", sourceEntries.none { it.phoneNumber == "+15550000006" })
    }

    @Test
    fun csvBatches_areActivatedAsOneAuthoritativeSnapshot() = runBlocking {
        val sourceId = database.sourceDao().insertSource(
            SourceEntity(name = "CSV Federal", type = "FTC", pathOrUrl = "test")
        ).toInt()
        val engine = DataSyncEngine(dataSourceRepository, SecureCsvParser())
        val csv = "+15550000007\n+15550000008\n+15550000009\n"

        val result = engine.replaceCsvSnapshot(
            inputStream = ByteArrayInputStream(csv.toByteArray()),
            sourceId = sourceId,
            securityRuleRepository = repository,
            batchSize = 2
        )

        assertTrue("CSV snapshot must be accepted", result is SnapshotActivationResult.Accepted)
        assertEquals(3, database.unifiedEntryDao().getEntryCountBySourceId(sourceId))
        assertEquals(
            SourceLifecycleState.HEALTHY.name,
            database.sourceDao().getSourceById(sourceId)!!.lifecycleState
        )
    }

    @Test
    fun xlsxBatches_areActivatedAsOneAuthoritativeSnapshot() = runBlocking {
        val sourceId = database.sourceDao().insertSource(
            SourceEntity(name = "XLSX Federal", type = "FTC", pathOrUrl = "test")
        ).toInt()
        val engine = DataSyncEngine(dataSourceRepository, SecureCsvParser())
        val xlsx = xlsxArchive(
            "<worksheet><sheetData>" +
                "<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>phone</t></is></c></row>" +
                "<row r=\"2\"><c r=\"A2\" t=\"inlineStr\"><is><t>+15550000010</t></is></c></row>" +
                "<row r=\"3\"><c r=\"A3\" t=\"inlineStr\"><is><t>+15550000011</t></is></c></row>" +
                "<row r=\"4\"><c r=\"A4\" t=\"inlineStr\"><is><t>+15550000012</t></is></c></row>" +
                "</sheetData></worksheet>"
        )

        val result = engine.replaceXlsxSnapshot(
            inputStream = ByteArrayInputStream(xlsx),
            sourceId = sourceId,
            securityRuleRepository = repository,
            batchSize = 2
        )

        assertTrue("XLSX snapshot must be accepted", result is SnapshotActivationResult.Accepted)
        assertEquals(3, database.unifiedEntryDao().getEntryCountBySourceId(sourceId))
        assertEquals(
            SourceLifecycleState.HEALTHY.name,
            database.sourceDao().getSourceById(sourceId)!!.lifecycleState
        )
    }

    private fun xlsxArchive(sheetXml: String): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zip.write(sheetXml.toByteArray())
            zip.closeEntry()
        }
        return output.toByteArray()
    }
}
