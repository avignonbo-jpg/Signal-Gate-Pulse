package com.signalgate.pulse.ui.notifications

import android.content.Context
import android.os.Vibrator
import android.os.VibratorManager
import com.signalgate.pulse.logic.HapticPolicy
import timber.log.Timber

/**
 * Phase 2.1 — sole haptic dispatcher for screening decisions.
 *
 * This is the only class in the application permitted to trigger vibration in
 * response to a screening outcome. [SignalGateCallScreeningService] calls
 * [dispatch] with the [HapticPolicy] from [ScreeningDecision.hapticPolicy];
 * this class resolves the pattern via [PulseVibration] and fires it.
 *
 * ## Security invariants
 *
 * - This class reads [HapticPolicy] from the immutable [ScreeningDecision] and
 *   never re-derives it from [CallTier], confidence, or any other field. The
 *   policy was set by [ScreeningDecision.forTier]; re-deriving it here would be
 *   a redundant and divergence-prone second decision.
 *
 * - [dispatch] is a no-op for [HapticPolicy.NONE]. No vibration occurs, no
 *   error is logged, no fallback fires. Silence is the correct behavior for
 *   ALLOWLISTED, FEDERAL_BLOCK, CLEAN_UNKNOWN, and SECURITY_FAILURE — each of
 *   those tiers explicitly requires NONE per the Phase 1.2 consequence table.
 *
 * - Rate limiting belongs to [PulseTriggerLimiter] (Phase 2.2), not here.
 *   [dispatch] fires whenever called. The limiter decides whether to call it.
 *   Do not add call-count checks or recency checks to this class.
 *
 * ## Platform compatibility
 *
 * minSdk 29 targets API 29. [VibratorManager] requires API 31; a fallback to
 * the deprecated [Vibrator] service is used for API 29–30. Both paths call the
 * same [VibrationEffect] produced by [PulseVibration], so the haptic behavior
 * is identical regardless of which system service is used.
 */
class PulseHapticsController(private val context: Context) {

    private val TAG = "PulseHaptics"

    /**
     * Fires the haptic pattern corresponding to [policy].
     *
     * Safe to call on any thread. No-op for [HapticPolicy.NONE].
     * Swallows platform exceptions — a vibration failure must never surface
     * as an unhandled exception in the screening pipeline.
     */
    fun dispatch(policy: HapticPolicy) {
        val effect = PulseVibration.forPolicy(policy) ?: return

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                        as? VibratorManager
                manager?.defaultVibrator?.vibrate(effect)
                    ?: Timber.tag(TAG).w("VibratorManager unavailable on API 31+ device")
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(effect)
                    ?: Timber.tag(TAG).w("Vibrator service unavailable on API <31 device")
            }
            Timber.tag(TAG).d("Haptic dispatched: $policy")
        } catch (e: Exception) {
            // Vibration is a UX enhancement. A failure here must not propagate
            // into the screening pipeline or affect audit/review-card writes.
            Timber.tag(TAG).e(e, "Haptic dispatch failed for policy $policy — swallowed")
        }
    }
}
