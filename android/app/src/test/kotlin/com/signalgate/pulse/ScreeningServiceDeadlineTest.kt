 package com.signalgate.pulse

import android.telecom.Call
import android.telecom.CallScreeningService.CallResponse
import com.signalgate.pulse.database.entities.CallLogEntry
import com.signalgate.pulse.logic.CallScreeningEngine
import com.signalgate.pulse.logic.ScreeningAction
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScreeningServiceDeadlineTest {

    private val service = SignalGateCallScreeningService()
    private val details: Call.Details = mock()
    private val engine: CallScreeningEngine = mock()
    private val responseFactory: (ScreeningAction) -> CallResponse = { mock() }

    private fun allowInfo() = CallInfo(
        originalPhoneNumber = "+15551234567",
        normalizedPhoneNumber = "+15551234567",
        spamStatus = "UNKNOWN",
        spamCategory = null,
        confidence = null,
        riskLevel = null,
        matchedSources = emptyList(),
        callDecision = ScreeningAction.ALLOW,
        tier = CallTier.CLEAN_UNKNOWN
    )

    @Test
    fun responseIsEmittedBeforePersistenceFinishes() = runBlocking {
        whenever(engine.screenCall(any(), any())).thenReturn(allowInfo())
        val response = CompletableDeferred<CallResponse>()
        val persistenceStarted = CompletableDeferred<Unit>()
        val releasePersistence = CompletableDeferred<Unit>()

        val processing = async {
            service.processScreeningCall(
                phoneNumber = "+15551234567",
                details = details,
                engine = engine,
                respond = { response.complete(it) },
                persist = { _, _ ->
                    persistenceStarted.complete(Unit)
                    releasePersistence.await()
                },
                dispatchUx = { _, _ -> },
                responseFactory = responseFactory
            )
        }

        withTimeout(1_000) { persistenceStarted.await() }
        val callResponse = withTimeout(1_000) { response.await() }
        assertFalse(callResponse.disallowCall)
        assertTrue("response must precede a blocked persistence operation", !processing.isCompleted)

        releasePersistence.complete(Unit)
        withTimeout(1_000) { processing.await() }
    }

    @Test
    fun responseIsEmittedWhenPersistenceThrows() = runBlocking {
        whenever(engine.screenCall(any(), any())).thenReturn(allowInfo())
        val responses = mutableListOf<CallResponse>()

        service.processScreeningCall(
            phoneNumber = "+15551234567",
            details = details,
            engine = engine,
            respond = { responses += it },
            persist = { _, _ -> throw IllegalStateException("database unavailable") },
            dispatchUx = { _, _ -> },
            responseFactory = responseFactory
        )

        assertEquals(1, responses.size)
        assertFalse(responses.single().disallowCall)
    }

    @Test
    fun nullHandleProducesExplicitResponseAndAuditedFailure() = runBlocking {
        whenever(details.handle).thenReturn(null)
        val response = CompletableDeferred<CallResponse>()
        val audit = CompletableDeferred<CallLogEntry>()

        service.handleScreeningRequest(
            details = details,
            respond = { response.complete(it) },
            launch = { error("null-handle path must not launch decision work") },
            onSecurityFailure = { phoneNumber ->
                service.handleSecurityFailure(
                    details = details,
                    phoneNumber = phoneNumber,
                    respond = { response.complete(it) },
                    audit = { audit.complete(it) },
                    responseFactory = responseFactory
                )
            }
        )

        val callResponse = withTimeout(1_000) { response.await() }
        val auditEntry = withTimeout(1_000) { audit.await() }
        assertFalse(callResponse.disallowCall)
        assertEquals("UNKNOWN_MALFORMED_HANDLE", auditEntry.phoneNumber)
        assertEquals(ScreeningAction.SECURITY_FAILURE.name, auditEntry.decision)
        assertEquals(CallTier.SECURITY_FAILURE.name, auditEntry.notes)
    }

    @Test
    fun unexpectedScreeningException_invokesSecurityFailureExactlyOnce() = runBlocking {
        var failureCount = 0
        var failurePhone: String? = null

        service.executeScreeningSafely(
            phoneNumber = "+15551234567",
            onSecurityFailure = { phoneNumber ->
                failureCount++
                failurePhone = phoneNumber
            }
        ) {
            throw IllegalStateException("unexpected screening failure")
        }

        assertEquals(1, failureCount)
        assertEquals("+15551234567", failurePhone)
    }
}
