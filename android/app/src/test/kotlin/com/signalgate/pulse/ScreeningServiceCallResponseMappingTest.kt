package com.signalgate.pulse

import android.telecom.CallScreeningService.CallResponse
import com.signalgate.pulse.logic.ScreeningAction
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScreeningServiceCallResponseMappingTest {

    @Test
    fun securityFailureResponse_doesNotDisallowCall() {
        val mapper = SignalGateCallScreeningService::class.java
            .getDeclaredMethod("toCallResponse", ScreeningAction::class.java)
            .apply { isAccessible = true }

        val response = mapper.invoke(
            SignalGateCallScreeningService(),
            ScreeningAction.SECURITY_FAILURE
        ) as CallResponse

        assertFalse("SECURITY_FAILURE must not disallow the call", response.disallowCall)
    }
}
