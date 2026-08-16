package com.signalgate.pulse.logic

import com.signalgate.pulse.CallInfo
import com.signalgate.pulse.CallTier
import com.signalgate.pulse.SignalGateCallScreeningService
import com.signalgate.pulse.database.repositories.DataSourceRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Phase 0.6 / INV-003 regression coverage.
 *
 * A failure while obtaining the authoritative security decision must remain an
 * explicit SECURITY_FAILURE result. It must never be converted into the
 * trusted CLEAN_UNKNOWN/ALLOW default path.
 */
class CallScreeningEngineSecurityFailureTest {

    @Test
    fun repositoryFailure_returnsExplicitSecurityFailure() = runBlocking {
        val repository = mock<DataSourceRepository>()
        whenever(repository.getCallDecision(any())).thenThrow(
            IllegalStateException("database unavailable")
        )

        val result: CallInfo = CallScreeningEngine(repository).screenCall(
            phoneNumber = "+15551234567",
            callDetails = null
        )

        assertEquals(CallTier.SECURITY_FAILURE, result.tier)
        assertEquals(
            SignalGateCallScreeningService.CallDecision.SECURITY_FAILURE,
            result.callDecision
        )
        assertEquals("SECURITY_FAILURE", result.spamStatus)
    }
}
