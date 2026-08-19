package com.signalgate.pulse

import android.app.Application
import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificationPrivacyTest {

    private val phoneNumber = "+15551234567"

    @Test
    fun blockedNotification_isPrivateAndRedactedOnLockScreenAndMirrors() {
        val notification = SignalGateCallScreeningService().buildBlockedCallNotification(
            context = ApplicationProvider.getApplicationContext<Application>(),
            callInfo = blockedCallInfo()
        )

        assertEquals(NotificationCompat.VISIBILITY_PRIVATE, notification.visibility)
        assertNotificationDoesNotExpose(notification, phoneNumber)
        assertNotNull("A redacted public version is required", notification.publicVersion)
        assertNotificationDoesNotExpose(notification.publicVersion!!, phoneNumber)
        assertTrue("The existing action must remain present", notification.actions.isNotEmpty())
    }

    @Test
    fun reviewNotification_isPrivateAndRedactedOnLockScreenAndMirrors() {
        val notification = SignalGateCallScreeningService().buildReviewAvailableNotification(
            context = ApplicationProvider.getApplicationContext<Application>(),
            callInfo = blockedCallInfo()
        )

        assertEquals(NotificationCompat.VISIBILITY_PRIVATE, notification.visibility)
        assertNotificationDoesNotExpose(notification, phoneNumber)
        assertNotNull("A redacted public version is required", notification.publicVersion)
        assertNotificationDoesNotExpose(notification.publicVersion!!, phoneNumber)
        assertFalse("Review notification has no feature action button", notification.actions.isNotEmpty())
    }

    private fun blockedCallInfo() = CallInfo(
        originalPhoneNumber = phoneNumber,
        normalizedPhoneNumber = phoneNumber,
        spamStatus = CallTier.HEURISTIC_BLOCK.name,
        spamCategory = "blocked",
        confidence = 90,
        riskLevel = "HIGH",
        matchedSources = listOf("test-source"),
        callDecision = com.signalgate.pulse.logic.ScreeningAction.BLOCK,
        tier = CallTier.HEURISTIC_BLOCK
    )

    private fun assertNotificationDoesNotExpose(notification: Notification, rawPhoneNumber: String) {
        val text = buildString {
            append(notification.extras?.getCharSequence(Notification.EXTRA_TITLE) ?: "")
            append(notification.extras?.getCharSequence(Notification.EXTRA_TEXT) ?: "")
            append(notification.extras?.getCharSequence(Notification.EXTRA_SUB_TEXT) ?: "")
        }
        assertFalse("Raw phone number must not appear in notification content", text.contains(rawPhoneNumber))
    }
}
