package com.signalgate.pulse.ui.notifications

import com.signalgate.pulse.logic.HapticPolicy
import com.signalgate.pulse.logic.NotificationPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PulseTriggerLimiterTest {

    private lateinit var limiter: PulseTriggerLimiter

    @Before
    fun setUp() {
        limiter = PulseTriggerLimiter()
    }

    @Test
    fun reviewNotification_isAllowedOncePerCooldownWindow() {
        val number = "+15551234567"
        val start = 1_700_000_000_000L

        assertTrue(limiter.shouldDispatchNotification(NotificationPolicy.REVIEW_AVAILABLE, number, start))
        assertFalse(
            limiter.shouldDispatchNotification(
                NotificationPolicy.REVIEW_AVAILABLE,
                number,
                start + PulseTriggerLimiter.COOLDOWN_MS - 1
            )
        )
        assertTrue(
            limiter.shouldDispatchNotification(
                NotificationPolicy.REVIEW_AVAILABLE,
                number,
                start + PulseTriggerLimiter.COOLDOWN_MS
            )
        )
    }

    @Test
    fun blockReviewAndBlockPulse_areNeverRateLimited() {
        val number = "+15551234567"
        val start = 1_700_000_000_000L

        assertTrue(limiter.shouldDispatchNotification(NotificationPolicy.BLOCK_REVIEW, number, start))
        assertTrue(limiter.shouldDispatchNotification(NotificationPolicy.BLOCK_REVIEW, number, start + 1))
        assertTrue(limiter.shouldDispatchHaptic(HapticPolicy.BLOCK_PULSE, number, start))
        assertTrue(limiter.shouldDispatchHaptic(HapticPolicy.BLOCK_PULSE, number, start + 1))
    }

    @Test
    fun nonePolicies_neverDispatch() {
        val number = "+15551234567"
        val now = 1_700_000_000_000L

        assertFalse(limiter.shouldDispatchNotification(NotificationPolicy.NONE, number, now))
        assertFalse(limiter.shouldDispatchHaptic(HapticPolicy.NONE, number, now))
    }

    @Test
    fun resetCooldown_allowsTheNextReviewNotification() {
        val number = "+15551234567"
        val now = 1_700_000_000_000L

        assertTrue(limiter.shouldDispatchNotification(NotificationPolicy.REVIEW_AVAILABLE, number, now))
        assertFalse(limiter.shouldDispatchNotification(NotificationPolicy.REVIEW_AVAILABLE, number, now + 1))

        limiter.resetCooldown(number)

        assertTrue(limiter.shouldDispatchNotification(NotificationPolicy.REVIEW_AVAILABLE, number, now + 1))
    }
}

