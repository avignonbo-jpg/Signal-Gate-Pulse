package com.signalgate.pulse.logic

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.signalgate.pulse.data.security.SecureCsvParser
import com.signalgate.pulse.data.security.SnapshotSanityValidator
import com.signalgate.pulse.database.SignalGateDatabase
import com.signalgate.pulse.database.daos.SourceDao
import com.signalgate.pulse.database.entities.SourceEntity
import com.signalgate.pulse.database.repositories.DataSourceRepository
import com.signalgate.pulse.database.repositories.SyncHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
class ReliableSourceManagerEnsureFederalRowsTest {

    private lateinit var database: SignalGateDatabase
    private lateinit var sourceDao: SourceDao
    private lateinit var databaseExecutor: ExecutorService

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        databaseExecutor = Executors.newFixedThreadPool(4)
        database = Room.inMemoryDatabaseBuilder(app, SignalGateDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(databaseExecutor)
            .setTransactionExecutor(databaseExecutor)
            .build()
        sourceDao = database.sourceDao()
    }

    @After
    fun tearDown() {
        database.close()
        databaseExecutor.shutdownNow()
    }

    @Test
    fun ensureFederalRows_onEmptyDatabaseCreatesOnlyFtcAndFccAndIsIdempotent(): Unit = runBlocking {
        val manager = createManager()

        manager.ensureFederalRows()
        val firstRows = sourceDao.getAllSources().first()

        assertEquals(2, firstRows.size)
        assertEquals(
            listOf("FTC Do Not Call Registry", "FCC Consumer Complaints"),
            firstRows.map { it.name }
        )
        assertTrue(firstRows.all { it.isEnabled })
        assertTrue(firstRows.all { it.lifecycleState == "ENABLED" })
        assertEquals(
            firstRows.map { it.id },
            SourceSyncUseCase.enabledFederalSourceIds(firstRows)
        )

        manager.ensureFederalRows()
        val secondRows = sourceDao.getAllSources().first()

        assertEquals(2, secondRows.size)
        assertEquals(firstRows, secondRows)
    }

    @Test
    fun ensureFederalRows_preservesAnExistingDisabledRowUnchanged(): Unit = runBlocking {
        val existingDisabledFtc = SourceEntity(
            name = "FTC Do Not Call Registry",
            type = "FTC",
            pathOrUrl = "user-preserved-url",
            isEnabled = false,
            priority = 17,
            lifecycleState = "DISABLED"
        )
        val insertedId = sourceDao.insertSource(existingDisabledFtc).toInt()
        val expectedExistingRow = existingDisabledFtc.copy(id = insertedId)

        createManager().ensureFederalRows()

        assertEquals(expectedExistingRow, sourceDao.getSourceByName(existingDisabledFtc.name))
        assertEquals(2, sourceDao.getAllSources().first().size)
        assertFalse(sourceDao.getSourceByName(existingDisabledFtc.name)!!.isEnabled)
    }

    @Test
    fun concurrentEnsureCallsCreateExactlyOneRowPerFederalSource(): Unit = runBlocking {
        val manager = createManager()

        coroutineScope {
            awaitAll(
                async(Dispatchers.IO) { manager.ensureFederalRows() },
                async(Dispatchers.IO) { manager.ensureFederalRows() }
            )
        }

        val rows = sourceDao.getAllSources().first()
        assertEquals(2, rows.size)
        assertEquals(1, rows.count { it.type == "FTC" })
        assertEquals(1, rows.count { it.type == "FCC" })
    }

    private fun createManager(): ReliableSourceManager {
        val repository = mock<DataSourceRepository>()
        runBlocking {
            whenever(repository.getSourceByName(any())).thenAnswer { invocation ->
                runBlocking {
                    sourceDao.getSourceByName(invocation.getArgument<String>(0))
                }
            }
            whenever(repository.insertSourceIfAbsent(any())).thenAnswer { invocation ->
                runBlocking {
                    sourceDao.insertSourceIfAbsent(invocation.getArgument<SourceEntity>(0))
                }
            }
        }

        return ReliableSourceManager(
            dataSourceRepository = repository,
            securityRuleRepository = mock(),
            syncHistoryRepository = mock<SyncHistoryRepository>(),
            secureCsvParser = mock<SecureCsvParser>(),
            snapshotSanityValidator = mock<SnapshotSanityValidator>()
        )
    }
}
