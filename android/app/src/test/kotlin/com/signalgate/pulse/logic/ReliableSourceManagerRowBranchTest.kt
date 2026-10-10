package com.signalgate.pulse.logic

import com.signalgate.pulse.database.entities.SourceEntity
import com.signalgate.pulse.database.repositories.DataSourceRepository
import com.signalgate.pulse.database.repositories.SyncHistoryRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import java.util.concurrent.atomic.AtomicInteger

class ReliableSourceManagerRowBranchTest {

    @Test
    fun rowCreationMutex_serializesLookupAndInsert(): Unit = runBlocking {
        val repository = mock<DataSourceRepository>()
        val firstEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val secondEntered = CompletableDeferred<Unit>()
        val activeEntries = AtomicInteger(0)
        val maxConcurrentEntries = AtomicInteger(0)
        val ftcLookupCalls = AtomicInteger(0)
        val ftcInsertCalls = AtomicInteger(0)

        whenever(repository.getSourceByName(any())).thenAnswer { invocation ->
            val name = invocation.getArgument<String>(0)
            if (name == FTC_NAME) {
                val call = ftcLookupCalls.incrementAndGet()
                val active = activeEntries.incrementAndGet()
                maxConcurrentEntries.updateAndGet { current -> maxOf(current, active) }
                if (call >= 2) {
                    secondEntered.complete(Unit)
                }
                try {
                    if (call == 1) {
                        firstEntered.complete(Unit)
                        runBlocking { releaseFirst.await() }
                        null
                    } else {
                        managedSource(id = 1, name = FTC_NAME, type = "FTC")
                    }
                } finally {
                    activeEntries.decrementAndGet()
                }
            } else {
                managedSource(id = 2, name = FCC_NAME, type = "FCC")
            }
        }
        whenever(repository.insertSourceIfAbsent(any())).thenAnswer { invocation ->
            val source = invocation.getArgument<SourceEntity>(0)
            if (source.name == FTC_NAME) {
                ftcInsertCalls.incrementAndGet()
                1L
            } else {
                2L
            }
        }

        val manager = createManager(repository)
        coroutineScope {
            val first = async(Dispatchers.IO) { manager.ensureFederalRows() }
            withTimeout(1_000) { firstEntered.await() }
            val second = async(Dispatchers.IO) { manager.ensureFederalRows() }
            withTimeoutOrNull(1_000) { secondEntered.await() }
            releaseFirst.complete(Unit)
            first.await()
            second.await()
        }

        assertEquals(1, maxConcurrentEntries.get())
        assertEquals(1, ftcInsertCalls.get())
    }

    @Test
    fun insertIgnored_returnsExistingRowWithoutThrowing(): Unit = runBlocking {
        val repository = mock<DataSourceRepository>()
        val ftcLookupCalls = AtomicInteger(0)
        val ftcInsertCalls = AtomicInteger(0)
        val disabledFtc = managedSource(
            id = 7,
            name = FTC_NAME,
            type = "FTC",
            isEnabled = false
        )

        whenever(repository.getSourceByName(any())).thenAnswer { invocation ->
            val name = invocation.getArgument<String>(0)
            if (name == FTC_NAME) {
                if (ftcLookupCalls.incrementAndGet() == 1) null else disabledFtc
            } else {
                managedSource(id = 8, name = FCC_NAME, type = "FCC")
            }
        }
        whenever(repository.insertSourceIfAbsent(any())).thenAnswer { invocation ->
            val source = invocation.getArgument<SourceEntity>(0)
            if (source.name == FTC_NAME) {
                ftcInsertCalls.incrementAndGet()
                -1L
            } else {
                8L
            }
        }

        createManager(repository).ensureFederalRows()

        assertEquals(1, ftcInsertCalls.get())
        assertEquals(2, ftcLookupCalls.get())
        verify(repository, never()).updateSource(any())
    }

    @Test
    fun concurrentRemovalBetweenInsertAndReread_retriesOnceAndReturnsNewRow(): Unit = runBlocking {
        val repository = mock<DataSourceRepository>()
        val ftcLookupCalls = AtomicInteger(0)
        val ftcInsertCalls = AtomicInteger(0)

        whenever(repository.getSourceByName(any())).thenAnswer { invocation ->
            val name = invocation.getArgument<String>(0)
            if (name == FTC_NAME) {
                ftcLookupCalls.incrementAndGet()
                null
            } else {
                managedSource(id = 8, name = FCC_NAME, type = "FCC")
            }
        }
        whenever(repository.insertSourceIfAbsent(any())).thenAnswer { invocation ->
            val source = invocation.getArgument<SourceEntity>(0)
            if (source.name == FTC_NAME) {
                if (ftcInsertCalls.incrementAndGet() == 1) -1L else 9L
            } else {
                8L
            }
        }

        createManager(repository).ensureFederalRows()

        assertEquals(2, ftcLookupCalls.get())
        assertEquals(2, ftcInsertCalls.get())
    }

    @Test
    fun unrecoverableEnsure_logsAndDoesNotThrow(): Unit = runBlocking {
        val repository = mock<DataSourceRepository>()
        val ftcLookupCalls = AtomicInteger(0)
        val ftcInsertCalls = AtomicInteger(0)
        val fccLookupCalls = AtomicInteger(0)

        whenever(repository.getSourceByName(any())).thenAnswer { invocation ->
            val name = invocation.getArgument<String>(0)
            if (name == FTC_NAME) {
                ftcLookupCalls.incrementAndGet()
                null
            } else {
                fccLookupCalls.incrementAndGet()
                managedSource(id = 8, name = FCC_NAME, type = "FCC")
            }
        }
        whenever(repository.insertSourceIfAbsent(any())).thenAnswer { invocation ->
            val source = invocation.getArgument<SourceEntity>(0)
            if (source.name == FTC_NAME) {
                ftcInsertCalls.incrementAndGet()
                -1L
            } else {
                8L
            }
        }

        createManager(repository).ensureFederalRows()

        assertEquals(3, ftcLookupCalls.get())
        assertEquals(2, ftcInsertCalls.get())
        assertEquals(1, fccLookupCalls.get())
    }

    private fun createManager(repository: DataSourceRepository): ReliableSourceManager =
        ReliableSourceManager(
            dataSourceRepository = repository,
            securityRuleRepository = mock(),
            syncHistoryRepository = mock<SyncHistoryRepository>(),
            secureCsvParser = mock(),
            snapshotSanityValidator = mock()
        )

    private fun managedSource(
        id: Int,
        name: String,
        type: String,
        isEnabled: Boolean = true
    ): SourceEntity = SourceEntity(
        id = id,
        name = name,
        type = type,
        pathOrUrl = "managed-$type",
        isEnabled = isEnabled
    )

    private companion object {
        const val FTC_NAME = "FTC Do Not Call Registry"
        const val FCC_NAME = "FCC Consumer Complaints"
    }
}
