package com.signalgate.pulse.ui.notifications

import com.signalgate.pulse.logic.HapticPolicy
import com.signalgate.pulse.logic.NotificationPolicy
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Phase 2.2 — UX rate limiter for notification and haptic dispatch.
 *
 * [PulseTriggerLimiter] is a throttle, not a security control. It may suppress
 * repeated notifications and haptics for the same phone number within a rolling
 * window, but it must NEVER suppress:
 *
 *   - the domain screening decision ([ScreeningDecision.callAction])
 *   - the audit record ([CallLogEntry])
 *   - the review card ([PendingCardEntity] when [ScreeningDecision.reviewCardRequired])
 *
 * Those three consequences are written by [SignalGateCallScreeningService.executeDecisionConsequences]
 * before this limiter is consulted. The limiter only gates [PulseHapticsController.dispatch]
 * and the notification-dispatch path. A call that is rate-limited still has a
 * full audit trail and a pending review card — the user just doesn't get a
 * repeated buzz or banner.
 *
 * ## Design
 *
 * Per-number cooldown: if a number triggers a gray-zone outcome more than once
 * within [COOLDOWN_MS], the second and subsequent notifications/haptics are
 * suppressed for that number until the window expires. This covers the realistic
 * spam pattern where the same number calls repeatedly in a short window.
 *
 * [NotificationPolicy.BLOCK_REVIEW] is never rate-limited — a HEURISTIC_BLOCK
 * represents a stronger signal than HEURISTIC_FLAG, and the user explicitly
 * needs visibility. Only [NotificationPolicy.REVIEW_AVAILABLE] (HEURISTIC_FLAG)
 * and [HapticPolicy.REVIEW_PULSE] are throttled.
 *
 * The in-memory map resets on process death. That is intentional — a fresh
 * process start means a fresh screening session, and the cooldown state from
 * a prior session is not security-relevant.
 *
 * Thread safety: [ConcurrentHashMap] is used directly; no external locking.
 */
class PulseTriggerLimiter {

    companion object {
        /** Cooldown window in milliseconds. Default: 5 minutes. */
        const val COOLDOWN_MS = 5 * 60 * 1_000L

        private const val TAG = "PulseTriggerLimiter"
    }

    /** Maps normalizedPhoneNumber → epoch-ms of last gray-zone dispatch. */
    private val lastReviewDispatch = ConcurrentHashMap<String, Long>()

    /**
     * Returns true if a [NotificationPolicy.REVIEW_AVAILABLE] notification
     * should be dispatched for [normalizedPhoneNumber] at [now].
     *
     * BLOCK_REVIEW is never limited and always returns true.
     * NONE always returns false (no dispatch needed regardless of rate limit).
     */
    fun shouldDispatchNotification(
        policy: NotificationPolicy,
        normalizedPhoneNumber: String,
        now: Long = System.currentTimeMillis()
    ): Boolean = when (policy) {
        NotificationPolicy.BLOCK_REVIEW    -> true   // never limited
        NotificationPolicy.REVIEW_AVAILABLE -> isOutsideCooldown(normalizedPhoneNumber, now)
            .also { allowed ->
                if (allowed) {
                    lastReviewDispatch[normalizedPhoneNumber] = now
                    Timber.tag(TAG).d("Review notification allowed")
                } else {
                    Timber.tag(TAG).d(
                        "Review notification suppressed within " +
                        "${COOLDOWN_MS / 1000}s cooldown"
                    )
                }
            }
        NotificationPolicy.NONE            -> false  // policy says no dispatch
    }

    /**
     * Returns true if a [HapticPolicy.REVIEW_PULSE] haptic should be dispatched
     * for [normalizedPhoneNumber] at [now].
     *
     * BLOCK_PULSE is never limited. NONE always returns false.
     *
     * The review haptic shares the same cooldown map as the review notification —
     * if the notification was suppressed for this number, the haptic is too,
     * and vice versa. They are co-throttled deliberately: a haptic without a
     * visible notification is confusing, and a notification without a haptic
     * when one was expected is inconsistent.
     */
    fun shouldDispatchHaptic(
        policy: HapticPolicy,
        normalizedPhoneNumber: String,
        now: Long = System.currentTimeMillis()
    ): Boolean = when (policy) {
        HapticPolicy.BLOCK_PULSE  -> true   // never limited
        HapticPolicy.REVIEW_PULSE -> isOutsideCooldown(normalizedPhoneNumber, now)
        HapticPolicy.NONE         -> false
    }

    /**
     * Clears the cooldown entry for [normalizedPhoneNumber].
     *
     * Called when the user acts on a review card (e.g. "Not Spam" via
     * [CallActionReceiver]) — if they've engaged with the card, they should
     * receive the next haptic/notification for the same number without waiting
     * for the cooldown to expire.
     *
     * This does not affect the audit record or review card for any prior call.
     */
    fun resetCooldown(normalizedPhoneNumber: String) {
        lastReviewDispatch.remove(normalizedPhoneNumber)
        Timber.tag(TAG).d("Cooldown reset")
    }

    /** Clears all cooldown state. Intended for testing only. */
    internal fun clearAll() = lastReviewDispatch.clear()

    private fun isOutsideCooldown(normalizedPhoneNumber: String, now: Long): Boolean {
        val last = lastReviewDispatch[normalizedPhoneNumber] ?: return true
        return (now - last) >= COOLDOWN_MS
    }
}
