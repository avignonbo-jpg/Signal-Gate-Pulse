package com.signalgate.pulse

import android.telecom.Call
import android.telecom.CallScreeningService.CallResponse
import com.signalgate.pulse.logic.CallScreeningEngine
import com.signalgate.pulse.logic.ScreeningAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
class ScreeningServiceTimingBudgetTest {

    private val service = SignalGateCallScreeningService()
    private val details: Call.Details = mock()
    private val engine: CallScreeningEngine = mock()

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
    fun measuredEngineLatenciesStayInsideTheDecisionBudgetOrEmitSecurityFailure() {
        val measured = listOf(500L, 2_000L, 3_400L, 3_600L).map { delayMs ->
            val actions = mutableListOf<ScreeningAction>()
            val elapsedMs = measureTimeMillis {
                runBlocking {
                    service.executeScreeningSafely(
                        phoneNumber = "+15551234567",
                        onSecurityFailure = {
                            actions += ScreeningAction.SECURITY_FAILURE
                        }
                    ) {
                        service.processScreeningCall(
                            phoneNumber = "+15551234567",
                            details = details,
                            engine = engine,
                            respond = { },
                            persist = { _, _ -> },
                            dispatchUx = { _, _ -> },
                            responseFactory = { action ->
                                actions += action
                                mock<CallResponse>()
                            },
                            screen = {
                                delay(delayMs)
                                allowInfo()
                            }
                        )
                    }
                }
            }

            val expected = if (delayMs >= 3_500L) {
                listOf(ScreeningAction.SECURITY_FAILURE)
            } else {
                listOf(ScreeningAction.ALLOW)
            }
            assertEquals("delay=${delayMs}ms measured=${elapsedMs}ms", expected, actions)
            assertTrue(
                "delay=${delayMs}ms measured=${elapsedMs}ms must resolve near the 3.5s budget",
                elapsedMs < 3_900L
            )
            if (delayMs < 3_500L) {
                assertTrue(
                    "delay=${delayMs}ms measured=${elapsedMs}ms must finish before 3.5s",
                    elapsedMs < 3_500L
                )
            }
            println("timing-budget delay_ms=$delayMs measured_ms=$elapsedMs action=${actions.last()}")
            delayMs to elapsedMs
        }

        assertEquals(listOf(500L, 2_000L, 3_400L, 3_600L), measured.map { it.first })
    }
}
