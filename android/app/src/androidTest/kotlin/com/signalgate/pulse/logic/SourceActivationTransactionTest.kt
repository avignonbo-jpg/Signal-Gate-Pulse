package com.signalgate.pulse.logic

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.signalgate.pulse.data.security.BloomFilterEngine
import com.signalgate.pulse.database.SignalGateDatabase
import com.signalgate.pulse.database.entities.SourceEntity
import com.signalgate.pulse.database.entities.UnifiedEntryEntity
import com.signalgate.pulse.database.repositories.DataSourceRepository
import com.signalgate.pulse.database.repositories.SettingRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
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

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, SignalGateDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dataSourceRepository = DataSourceRepository(
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
        val sourceId = database.sourceDao().insertSource(
            SourceEntity(name = "Test Federal", type = "FTC", pathOrUrl = "test")
        ).toInt()
        val oldEntry = UnifiedEntryEntity(
            phoneNumber = "+15550000001",
            action = "BLOCK",
            sourceId = sourceId
        )
        database.unifiedEntryDao().insertEntry(oldEntry)

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
        assertTrue(
            "last-known-good entry must remain active",
            database.unifiedEntryDao().findEntriesBySourceId(sourceId).any { it.phoneNumber == oldEntry.phoneNumber }
        )
    }
}
