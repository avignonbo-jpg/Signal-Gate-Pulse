package com.signalgate.pulse.workers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.signalgate.pulse.logic.ReliableSourceManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CommunitySyncWorkerTest {

    private lateinit var context: Context
    private lateinit var manager: ReliableSourceManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        manager = mock()
        startKoin {
            modules(module {
                single<ReliableSourceManager> { manager }
            })
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun cancellationException_propagatesFromDoWork(): Unit = runBlocking {
        val cancellation = CancellationException("test cancellation")
        whenever(manager.syncAllFederalSources()).thenThrow(cancellation)

        var thrown: CancellationException? = null
        try {
            worker().doWork()
            fail("CancellationException should propagate from doWork")
        } catch (exception: CancellationException) {
            thrown = exception
        }

        assertNotNull(thrown)
        assertEquals(cancellation.message, thrown?.message)
    }

    @Test
    fun genericException_returnsFailure(): Unit = runBlocking {
        whenever(manager.syncAllFederalSources()).thenThrow(IllegalStateException("boom"))

        val result = worker().doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
    }

    @Test
    fun partialSync_returnsRetry(): Unit = runBlocking {
        whenever(manager.syncAllFederalSources()).thenReturn(
            listOf(
                ReliableSourceManager.SyncResult("FTC", 1, success = true),
                ReliableSourceManager.SyncResult("FCC", 0, success = false)
            )
        )

        val result = worker().doWork()

        assertTrue(result is ListenableWorker.Result.Retry)
    }

    private fun worker(): CommunitySyncWorker {
        val parameters = mock<WorkerParameters>(defaultAnswer = Answers.RETURNS_DEEP_STUBS)
        return CommunitySyncWorker(context, parameters)
    }
}
