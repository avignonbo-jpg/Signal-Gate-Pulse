package com.signalgate.pulse.logic

import android.os.Parcelable
import com.signalgate.pulse.CallTier
import kotlinx.parcelize.Parcelize

/**
 * Policy-semantic notification consequence. This names what the decision calls
 * for; it does not describe whether a platform dispatch has already occurred.
 */
enum class NotificationPolicy {
    NONE,
    BLOCK_REVIEW,
    REVIEW_AVAILABLE
}

/**
 * Policy-semantic haptic consequence. Dispatch and any future rate limiting are
 * edge concerns and must never change this value.
 */
enum class HapticPolicy {
    NONE,
    BLOCK_PULSE,
    REVIEW_PULSE
}

/**
 * Immutable decision contract carried from the decision engine to the edge.
 *
 * The edge executes these consequences; it must not reconstruct them from
 * CallTier, spamStatus, confidence, or source strings. SECURITY_FAILURE remains
 * a distinct domain action even though the Android response policy deliberately
 * rings through, preserving the Phase 0.6 invariant that failure is not ALLOW.
 */
@Parcelize
data class ScreeningDecision(
    val tier: CallTier,
    val callAction: ScreeningAction,
    val auditRequired: Boolean,
    val reviewCardRequired: Boolean,
    val notificationPolicy: NotificationPolicy,
    val hapticPolicy: HapticPolicy,
    val securityFailure: Boolean
) : Parcelable {
    init {
        require(securityFailure == (tier == CallTier.SECURITY_FAILURE)) {
            "securityFailure must match SECURITY_FAILURE tier"
        }
        require(securityFailure == (callAction == ScreeningAction.SECURITY_FAILURE)) {
            "securityFailure must match SECURITY_FAILURE action"
        }
    }

    companion object {
        fun forTier(tier: CallTier, action: ScreeningAction): ScreeningDecision {
            return when (tier) {
                CallTier.ALLOWLISTED -> ScreeningDecision(
                    tier, action, auditRequired = false, reviewCardRequired = false,
                    NotificationPolicy.NONE, HapticPolicy.NONE, securityFailure = false
                )
                CallTier.FEDERAL_BLOCK -> ScreeningDecision(
                    tier, action, auditRequired = true, reviewCardRequired = false,
                    NotificationPolicy.NONE, HapticPolicy.NONE, securityFailure = false
                )
                CallTier.HEURISTIC_BLOCK -> ScreeningDecision(
                    tier, action, auditRequired = true, reviewCardRequired = true,
                    NotificationPolicy.BLOCK_REVIEW, HapticPolicy.BLOCK_PULSE, securityFailure = false
                )
                CallTier.HEURISTIC_FLAG -> ScreeningDecision(
                    tier, action, auditRequired = true, reviewCardRequired = true,
                    NotificationPolicy.REVIEW_AVAILABLE, HapticPolicy.REVIEW_PULSE, securityFailure = false
                )
                CallTier.CLEAN_UNKNOWN -> ScreeningDecision(
                    tier, action, auditRequired = true, reviewCardRequired = false,
                    NotificationPolicy.NONE, HapticPolicy.NONE, securityFailure = false
                )
                CallTier.SECURITY_FAILURE -> ScreeningDecision(
                    tier, ScreeningAction.SECURITY_FAILURE, auditRequired = true,
                    reviewCardRequired = false, NotificationPolicy.NONE, HapticPolicy.NONE,
                    securityFailure = true
                )
            }
        }
    }
}
