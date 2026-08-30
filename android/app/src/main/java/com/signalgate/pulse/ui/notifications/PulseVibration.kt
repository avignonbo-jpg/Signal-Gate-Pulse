package com.signalgate.pulse.ui.notifications

import android.os.VibrationEffect
import com.signalgate.pulse.logic.HapticPolicy

/**
 * Vibration pattern definitions for Phase 2.1.
 *
 * Each [HapticPolicy] that requires a haptic maps to a named [VibrationEffect].
 * Patterns are defined here and only here — [PulseHapticsController] is the
 * sole dispatcher. No other class constructs a [VibrationEffect] for screening
 * feedback.
 *
 * minSdk 29 — [VibrationEffect.createWaveform] and [VibrationEffect.createOneShot]
 * are available from API 26; the project targets 29, so no version guard is needed.
 *
 * Design intent:
 *   BLOCK_PULSE  — two short pulses. Distinct from a standard ringtone; signals
 *                  "something was stopped" without being alarming.
 *   REVIEW_PULSE — one soft pulse. Signals "something needs your attention later"
 *                  without implying immediate action. Intentionally lighter than
 *                  BLOCK_PULSE so the two are perceptibly different.
 *
 * Phase 2.2 note: [PulseTriggerLimiter] gates whether [PulseHapticsController]
 * is called at all. It must never change the effect produced here — rate limiting
 * is a dispatch throttle, not a policy override. Do not branch on call count or
 * recency inside this file.
 */
object PulseVibration {

    /**
     * Two short pulses — 80ms on, 80ms off, 80ms on.
     * Retained as the effect for [HapticPolicy.BLOCK_PULSE]. It is not selected
     * by [ScreeningDecision.forTier] for HEURISTIC_BLOCK calls.
     */
    val blockPulse: VibrationEffect = VibrationEffect.createWaveform(
        longArrayOf(0L, 80L, 80L, 80L),   // delay, on, off, on
        -1                                  // do not repeat
    )

    /**
     * Single soft pulse — 60ms at half amplitude.
     * Used for [HapticPolicy.REVIEW_PULSE] (HEURISTIC_FLAG calls).
     */
    val reviewPulse: VibrationEffect = VibrationEffect.createOneShot(
        60L,
        VibrationEffect.DEFAULT_AMPLITUDE / 2
    )

    /**
     * Returns the [VibrationEffect] corresponding to [policy], or null if the
     * policy does not require a haptic. Null means the caller must not vibrate —
     * it is not an error condition.
     */
    fun forPolicy(policy: HapticPolicy): VibrationEffect? = when (policy) {
        HapticPolicy.BLOCK_PULSE  -> blockPulse
        HapticPolicy.REVIEW_PULSE -> reviewPulse
        HapticPolicy.NONE         -> null
    }
}
