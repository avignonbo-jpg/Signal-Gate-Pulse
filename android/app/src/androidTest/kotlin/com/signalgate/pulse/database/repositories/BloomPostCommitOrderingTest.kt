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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the write-side INV-001 boundary: Room is authoritative and Bloom is
 * derived. A committed authoritative write is invisible to a warm Bloom fast-pass
 * until rebuildDerivedIndexes() is invoked explicitly after the write completes.
 */
@RunWith(AndroidJUnit4::class)
class BloomPostCommitOrderingTest {

    private lateinit var database: SignalGateDatabase
    private lateinit var repository: DataSourceRepository
    private var sourceId: Int = 0

    @Before
    fun setUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, SignalGateDatabase::class.java).build()
        repository = DataSourceRepository(
            database.sourceDao(),
            database.unifiedEntryDao(),
            BloomFilterEngine(),
            BloomFilterEngine()
        )
        sourceId = database.sourceDao().insertSource(
            SourceEntity(
                name = "Post-Commit Test Source",
                type = "URL",
                pathOrUrl = "https://example.test",
                isEnabled = true,
                priority = 85,
                healthStatus = "HEALTHY"
            )
        ).toInt()
        repository.rehydrateBloomFilters()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun authoritativeWrite_doesNotMutateWarmBloom_untilExplicitRebuild() = runBlocking {
        val number = "+18005550999"
        repository.insertEntriesAuthoritative(
            listOf(
                UnifiedEntryEntity(
                    phoneNumber = number,
                    action = "BLOCK",
                    sourceId = sourceId
                )
            )
        )

        assertEquals(
            "Warm Bloom must not claim a newly committed row before rebuild",
            "ALLOW",
            repository.getCallDecision(number).action
        )

        repository.rebuildDerivedIndexes()

        assertEquals(
            "Explicit post-commit rebuild must expose the authoritative BLOCK row",
            "BLOCK",
            repository.getCallDecision(number).action
        )
    }
}
