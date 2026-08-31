package com.signalgate.pulse

import com.signalgate.pulse.logic.ScreeningAction
import org.junit.Assert.assertFalse
import org.junit.Test
class ScreeningServiceCallResponseMappingTest {

    @Test
    fun securityFailureResponse_doesNotDisallowCall() {
        val policy = SignalGateCallScreeningService().responsePolicy(ScreeningAction.SECURITY_FAILURE)

        assertFalse("SECURITY_FAILURE must not disallow the call", policy.disallowCall)
        assertFalse("SECURITY_FAILURE must not silence the call", policy.silenceCall)
        assertFalse("SECURITY_FAILURE must not skip the call log", policy.skipCallLog)
        assertFalse("SECURITY_FAILURE must not skip the notification", policy.skipNotification)
    }
}
