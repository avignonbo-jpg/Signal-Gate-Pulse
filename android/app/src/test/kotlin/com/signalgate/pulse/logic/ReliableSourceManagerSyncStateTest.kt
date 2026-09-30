package com.signalgate.pulse.logic

import com.signalgate.pulse.data.security.SecureCsvParser
import com.signalgate.pulse.data.security.SnapshotSanityValidator
import com.signalgate.pulse.database.entities.SourceEntity
import com.signalgate.pulse.database.repositories.DataSourceRepository
import com.signalgate.pulse.database.repositories.SyncHistoryRepository
import com.signalgate.pulse.ui.screens.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class ReliableSourceManagerSyncStateTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun overlappingSameSourceSyncs_areSerializedAndStaySyncingUntilBothFinish(): Unit =
        runTest(mainDispatcherRule.dispatcher) {
            val firstGate = RequestGate()
            val secondGate = RequestGate()
            val transport = ControlledTransport(ftcGates = listOf(firstGate, secondGate))
            val fixture = newFixture(transport.client)
            val lookupCount = AtomicInteger()
            val secondPublicLookup = CompletableDeferred<Unit>()
            whenever(fixture.dataSourceRepository.getSourceById(FTC_SOURCE.id)).thenAnswer {
                if (lookupCount.incrementAndGet() == 3) {
                    secondPublicLookup.complete(Unit)
                }
                FTC_SOURCE
            }

            val states = observe(fixture.manager.isSyncing)
            val first = async { fixture.manager.syncSource(FTC_SOURCE.id) }
            firstGate.entered.await()
            assertTrue(fixture.manager.isSyncing.first { it })

            val second = async { fixture.manager.syncSource(FTC_SOURCE.id) }
            secondPublicLookup.await()
            assertFalse(
                "second HTTP request must wait for the first lock holder",
                secondGate.enteredLatch.await(50, TimeUnit.MILLISECONDS)
            )
            assertFalse("second sync must still be waiting", second.isCompleted)

            firstGate.release.complete(Unit)
            secondGate.entered.await()
            first.await()
            assertTrue("sync state must remain true while the second sync runs", fixture.manager.isSyncing.first())

            secondGate.release.complete(Unit)
            second.await()
            assertFalse(fixture.manager.isSyncing.first())
            assertEquals(listOf(false, true, false), states.toList())
        }

    @Test
    fun syncSourcesBatch_doesNotDipToFalseBetweenSources(): Unit =
        runTest(mainDispatcherRule.dispatcher) {
            val ftcGate = RequestGate()
            val fccGate = RequestGate()
            val transport = ControlledTransport(
                ftcGates = listOf(ftcGate),
                fccGates = listOf(fccGate)
            )
            val fixture = newFixture(transport.client)
            whenever(fixture.dataSourceRepository.getSourceById(FTC_SOURCE.id)).thenReturn(FTC_SOURCE)
            whenever(fixture.dataSourceRepository.getSourceById(FCC_SOURCE.id)).thenReturn(FCC_SOURCE)
            val useCase = SourceSyncUseCase(fixture.manager, fixture.dataSourceRepository)
            val states = observe(useCase.isSyncing)

            val batch = async { useCase.syncSources(listOf(FTC_SOURCE.id, FCC_SOURCE.id)) }
            ftcGate.entered.await()
            assertTrue(useCase.isSyncing.first { it })

            ftcGate.release.complete(Unit)
            fccGate.entered.await()
            assertTrue("batch-level tracking must bridge the item boundary", useCase.isSyncing.first())
            assertEquals(listOf(false, true), states.toList())

            fccGate.release.complete(Unit)
            val result = batch.await()
            assertEquals(2, result.totalCount)
            assertFalse(useCase.isSyncing.first())
            assertEquals(listOf(false, true, false), states.toList())
        }

    @Test
    fun thrownPublicSync_clearsInFlightState(): Unit = runTest(mainDispatcherRule.dispatcher) {
        val fixture = newFixture(ControlledTransport().client)
        whenever(fixture.dataSourceRepository.getSourceById(404))
            .thenThrow(IllegalStateException("test repository failure"))
        val states = observe(fixture.manager.isSyncing)

        val failure = runCatching { fixture.manager.syncSource(404) }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertFalse(fixture.manager.isSyncing.first())
        assertEquals(listOf(false, true, false), states.toList())
    }

    @Test
    fun cancelledPublicSync_clearsInFlightState(): Unit = runTest(mainDispatcherRule.dispatcher) {
        val gate = RequestGate()
        val fixture = newFixture(ControlledTransport(ftcGates = listOf(gate)).client)
        whenever(fixture.dataSourceRepository.getSourceById(FTC_SOURCE.id)).thenReturn(FTC_SOURCE)
        val states = observe(fixture.manager.isSyncing)

        val sync = async { fixture.manager.syncSource(FTC_SOURCE.id) }
        gate.entered.await()
        assertTrue(fixture.manager.isSyncing.first { it })

        sync.cancel()
        gate.release.complete(Unit)
        sync.cancelAndJoin()

        assertFalse(fixture.manager.isSyncing.first())
        assertEquals(listOf(false, true, false), states.toList())
    }

    @Test
    fun syncCancelledWhileWaitingForLock_decrementsItsCount(): Unit =
        runTest(mainDispatcherRule.dispatcher) {
            val firstGate = RequestGate()
            val secondGate = RequestGate()
            val transport = ControlledTransport(ftcGates = listOf(firstGate, secondGate))
            val fixture = newFixture(transport.client)
            val lookupCount = AtomicInteger()
            val secondPublicLookup = CompletableDeferred<Unit>()
            whenever(fixture.dataSourceRepository.getSourceById(FTC_SOURCE.id)).thenAnswer {
                if (lookupCount.incrementAndGet() == 3) {
                    secondPublicLookup.complete(Unit)
                }
                FTC_SOURCE
            }

            val first = async { fixture.manager.syncSource(FTC_SOURCE.id) }
            firstGate.entered.await()
            val waitingSync = async { fixture.manager.syncSource(FTC_SOURCE.id) }
            secondPublicLookup.await()
            assertFalse(
                "the waiting call must not enter HTTP before the first lock holder finishes",
                secondGate.enteredLatch.await(50, TimeUnit.MILLISECONDS)
            )
            assertFalse("the second call should be queued behind the private sync mutex", waitingSync.isCompleted)

            waitingSync.cancelAndJoin()
            assertTrue("the first call remains in flight after the waiter is cancelled", fixture.manager.isSyncing.first())

            firstGate.release.complete(Unit)
            first.await()
            assertFalse(fixture.manager.isSyncing.first())
        }

    private suspend fun newFixture(client: OkHttpClient): Fixture {
        val dataSourceRepository = mock<DataSourceRepository>()
        val securityRuleRepository = mock<SecurityRuleRepository>()
        whenever(securityRuleRepository.beginSourceSync(any())).thenReturn(1L)
        val manager = ReliableSourceManager(
            dataSourceRepository = dataSourceRepository,
            securityRuleRepository = securityRuleRepository,
            syncHistoryRepository = mock<SyncHistoryRepository>(),
            secureCsvParser = mock<SecureCsvParser>(),
            snapshotSanityValidator = mock<SnapshotSanityValidator>(),
            httpClient = client
        )
        return Fixture(manager, dataSourceRepository)
    }

    private fun TestScope.observe(flow: Flow<Boolean>): CopyOnWriteArrayList<Boolean> {
        val states = CopyOnWriteArrayList<Boolean>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            flow.collect { states += it }
        }
        return states
    }

    private data class Fixture(
        val manager: ReliableSourceManager,
        val dataSourceRepository: DataSourceRepository
    )

    private class RequestGate {
        val entered = CompletableDeferred<Unit>()
        val enteredLatch = CountDownLatch(1)
        val release = CompletableDeferred<Unit>()
    }

    private class ControlledTransport(
        private val ftcGates: List<RequestGate> = emptyList(),
        private val fccGates: List<RequestGate> = emptyList()
    ) {
        private val ftcPrimaryCount = AtomicInteger()
        private val fccPrimaryCount = AtomicInteger()

        val client: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val gate = when {
                    request.isFtcPrimary() -> ftcGates.getOrNull(ftcPrimaryCount.getAndIncrement())
                    request.isFccPrimary() -> fccGates.getOrNull(fccPrimaryCount.getAndIncrement())
                    else -> null
                }
                gate?.let {
                    runBlocking {
                        it.entered.complete(Unit)
                        it.enteredLatch.countDown()
                        it.release.await()
                    }
                }
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(503)
                    .message("Injected test failure")
                    .body(ByteArray(0).toResponseBody("text/plain".toMediaType()))
                    .build()
            }
            .build()

        private fun Request.isFtcPrimary(): Boolean =
            url.host == "raw.githubusercontent.com" && url.encodedPath.endsWith("/dnc-numbers.json")

        private fun Request.isFccPrimary(): Boolean =
            url.host == "opendata.fcc.gov" && url.queryParameter("accessType") == "DOWNLOAD"
    }

    private companion object {
        val FTC_SOURCE = SourceEntity(
            id = 11,
            name = "FTC Do Not Call Registry",
            type = "FTC",
            pathOrUrl = "test-ftc",
            priority = 90
        )
        val FCC_SOURCE = SourceEntity(
            id = 12,
            name = "FCC Consumer Complaints",
            type = "FCC",
            pathOrUrl = "test-fcc",
            priority = 85
        )
    }
}
