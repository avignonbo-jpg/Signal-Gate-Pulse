package com.signalgate.pulse.logic

import com.signalgate.pulse.CallTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 1.2 consequence contract tests.
 *
 * These tests assert policy, not dispatch state. NotificationPolicy and
 * HapticPolicy describe what the decision requires; edge dispatch and future
 * rate limiting must not rewrite them.
 */
class ScreeningDecisionConsequencesTest {

    @Test
    fun allowlisted_requiresNoDownstreamReviewConsequences() {
        val decision = ScreeningDecision.forTier(CallTier.ALLOWLISTED, ScreeningAction.ALLOW)

        assertEquals(ScreeningAction.ALLOW, decision.callAction)
        assertFalse(decision.auditRequired)
        assertFalse(decision.reviewCardRequired)
        assertEquals(NotificationPolicy.NONE, decision.notificationPolicy)
        assertEquals(HapticPolicy.NONE, decision.hapticPolicy)
        assertFalse(decision.securityFailure)
    }

    @Test
    fun federalBlock_requiresAuditButNoGrayZoneReviewConsequences() {
        val decision = ScreeningDecision.forTier(CallTier.FEDERAL_BLOCK, ScreeningAction.BLOCK)

        assertEquals(ScreeningAction.BLOCK, decision.callAction)
        assertTrue(decision.auditRequired)
        assertFalse(decision.reviewCardRequired)
        assertEquals(NotificationPolicy.NONE, decision.notificationPolicy)
        assertEquals(HapticPolicy.NONE, decision.hapticPolicy)
        assertFalse(decision.securityFailure)
    }

    @Test
    fun heuristicBlock_requiresBlockReviewAndPulse() {
        val decision = ScreeningDecision.forTier(CallTier.HEURISTIC_BLOCK, ScreeningAction.BLOCK)

        assertEquals(ScreeningAction.BLOCK, decision.callAction)
        assertTrue(decision.auditRequired)
        assertTrue(decision.reviewCardRequired)
        assertEquals(NotificationPolicy.BLOCK_REVIEW, decision.notificationPolicy)
        assertEquals(HapticPolicy.BLOCK_PULSE, decision.hapticPolicy)
        assertFalse(decision.securityFailure)
    }

    @Test
    fun heuristicFlag_requiresReviewAvailableAndReviewPulse() {
        val decision = ScreeningDecision.forTier(CallTier.HEURISTIC_FLAG, ScreeningAction.SCREEN)

        assertEquals(ScreeningAction.SCREEN, decision.callAction)
        assertTrue(decision.auditRequired)
        assertTrue(decision.reviewCardRequired)
        assertEquals(NotificationPolicy.REVIEW_AVAILABLE, decision.notificationPolicy)
        assertEquals(HapticPolicy.REVIEW_PULSE, decision.hapticPolicy)
        assertFalse(decision.securityFailure)
    }

    @Test
    fun cleanUnknown_requiresAuditButNoReviewConsequences() {
        val decision = ScreeningDecision.forTier(CallTier.CLEAN_UNKNOWN, ScreeningAction.ALLOW)

        assertEquals(ScreeningAction.ALLOW, decision.callAction)
        assertTrue(decision.auditRequired)
        assertFalse(decision.reviewCardRequired)
        assertEquals(NotificationPolicy.NONE, decision.notificationPolicy)
        assertEquals(HapticPolicy.NONE, decision.hapticPolicy)
        assertFalse(decision.securityFailure)
    }

    @Test
    fun securityFailure_isDistinctFromAllowAndRemainsAuditable() {
        val decision = ScreeningDecision.forTier(
            CallTier.SECURITY_FAILURE,
            ScreeningAction.SECURITY_FAILURE
        )

        assertEquals(ScreeningAction.SECURITY_FAILURE, decision.callAction)
        assertTrue(decision.auditRequired)
        assertFalse(decision.reviewCardRequired)
        assertEquals(NotificationPolicy.NONE, decision.notificationPolicy)
        assertEquals(HapticPolicy.NONE, decision.hapticPolicy)
        assertTrue(decision.securityFailure)
    }

    @Test(expected = IllegalArgumentException::class)
    fun securityFailure_cannotBeConstructedAsOrdinaryAllow() {
        ScreeningDecision(
            tier = CallTier.SECURITY_FAILURE,
            callAction = ScreeningAction.ALLOW,
            auditRequired = true,
            reviewCardRequired = false,
            notificationPolicy = NotificationPolicy.NONE,
            hapticPolicy = HapticPolicy.NONE,
            securityFailure = true
        )
    }
}
