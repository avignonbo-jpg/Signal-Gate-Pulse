package com.signalgate.pulse.database.repositories

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.signalgate.pulse.data.security.BloomFilterEngine
import com.signalgate.pulse.database.SignalGateDatabase
import com.signalgate.pulse.database.entities.SourceEntity
import com.signalgate.pulse.database.entities.UnifiedEntryEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DataSourceRepositoryPersistedDeletionTest {

    private lateinit var database: SignalGateDatabase
    private lateinit var sourceDao: com.signalgate.pulse.database.daos.SourceDao
    private lateinit var entryDao: com.signalgate.pulse.database.daos.UnifiedEntryDao
    private lateinit var repository: DataSourceRepository

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(application, SignalGateDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sourceDao = database.sourceDao()
        entryDao = database.unifiedEntryDao()
        repository = DataSourceRepository(
            sourceDao = sourceDao,
            entryDao = entryDao,
            bloomFilter = BloomFilterEngine(),
            patternBloomFilter = BloomFilterEngine()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun sameIdCsvCopyOfProtectedPersistedSource_isRefusedAndPreservesRowAndEntries(): Unit = runBlocking {
        val persistedId = sourceDao.insertSource(
            SourceEntity(
                name = "Manual User Rules",
                type = "MANUAL",
                pathOrUrl = "local",
                isEnabled = true,
                priority = 100
            )
        ).toInt()
        entryDao.insertEntry(
            UnifiedEntryEntity(
                phoneNumber = "+18005551234",
                action = "BLOCK",
                sourceId = persistedId
            )
        )
        val persisted = sourceDao.getSourceById(persistedId)!!
        val spoofedCallerCopy = persisted.copy(
            name = "Caller-supplied CSV copy",
            type = "CSV"
        )

        val exception = assertThrows(ProtectedSourceDeletionException::class.java) {
            runBlocking { repository.deleteSource(spoofedCallerCopy) }
        }

        assertEquals("MANUAL", persisted.type)
        assertEquals(true, exception.message?.contains("Manual User Rules") == true)
        assertEquals(persisted, sourceDao.getSourceById(persistedId))
        assertEquals(1, entryDao.getEntryCountBySourceId(persistedId))
    }

    @Test
    fun nonProtectedPersistedSource_isDeletedByIdAndCascadesEntries(): Unit = runBlocking {
        val sourceId = sourceDao.insertSource(
            SourceEntity(
                name = "CSV Source",
                type = "CSV",
                pathOrUrl = "local.csv",
                isEnabled = true,
                priority = 10
            )
        ).toInt()
        entryDao.insertEntry(
            UnifiedEntryEntity(
                phoneNumber = "+18005556789",
                action = "BLOCK",
                sourceId = sourceId
            )
        )
        val persisted = sourceDao.getSourceById(sourceId)!!

        repository.deleteSource(persisted.copy(name = "stale caller name"))

        assertNull(sourceDao.getSourceById(sourceId))
        assertEquals(0, entryDao.getEntryCountBySourceId(sourceId))
    }

    @Test
    fun nonExistentId_isAnIdempotentNoOp(): Unit = runBlocking {
        val missing = SourceEntity(
            id = 987654,
            name = "Missing CSV Source",
            type = "CSV",
            pathOrUrl = "missing.csv"
        )

        repository.deleteSource(missing)

        assertNull(sourceDao.getSourceById(missing.id))
        assertEquals(0, entryDao.getEntryCountBySourceId(missing.id))
    }
}
