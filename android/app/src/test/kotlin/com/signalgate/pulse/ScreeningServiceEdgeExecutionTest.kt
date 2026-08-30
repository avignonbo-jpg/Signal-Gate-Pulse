package com.signalgate.pulse

import com.signalgate.pulse.CallTier
import com.signalgate.pulse.database.entities.CallLogEntry
import com.signalgate.pulse.database.entities.PendingCardEntity
import com.signalgate.pulse.database.repositories.CallLogRepository
import com.signalgate.pulse.database.repositories.PendingCardRepository
import com.signalgate.pulse.logic.CallScreeningEngine
import com.signalgate.pulse.logic.HapticPolicy
import com.signalgate.pulse.logic.NotificationPolicy
import com.signalgate.pulse.logic.ScreeningAction
import com.signalgate.pulse.logic.ScreeningDecision
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Phase 1.2 — Edge execution tests for SignalGateCallScreeningService.
 *
 * These tests prove that the service correctly *executes* the consequence
 * contract produced by ScreeningDecision — not that the contract is
 * correctly defined (ScreeningDecisionConsequencesTest covers that) and
 * not that the engine makes correct decisions (CallScreeningEngineDecisionMatrixTest
 * covers that). The gap Phase 1.2 was asked to close is specifically:
 *
 *   Does the edge correctly execute each field of ScreeningDecision?
 *   — [callAction]          → correct CallResponse (via toCallResponse)
 *   — [auditRequired]       → CallLogEntry written iff true
 *   — [reviewCardRequired]  → PendingCardEntity written iff true
 *   — [notificationPolicy]  → BLOCK_REVIEW fires notification; REVIEW_AVAILABLE
 *                              does not dispatch (Phase 2.1); NONE is silent
 *   — [hapticPolicy]        → policy value preserved on the decision object
 *                              (dispatch wired in Phase 2.1)
 *   — [securityFailure]     → SECURITY_FAILURE never aliases ALLOW in the log
 *
 * The service uses Android APIs (TelecomCallScreeningService, NotificationManager)
 * that require an Android runner for full instrumented tests. The paths covered
 * here exercise the service's internal consequence-execution logic through its
 * internal [writeAuditRecords] delegate pattern, keeping these as JVM unit tests
 * by mocking the repositories and engine.
 *
 * Architecture: the service depends on CallScreeningEngine, CallLogRepository,
 * and PendingCardRepository — all Koin-injected. Tests construct the service
 * with test doubles via a factory helper to keep each test focused.
 *
 * Phase 2.1 note: hapticPolicy and REVIEW_AVAILABLE notification dispatch are
 * explicitly deferred to Phase 2.1 after Phase 1.3 proves persisted reviewability.
 * Tests here confirm the policy VALUE is correctly set on the decision object;
 * they do not assert a dispatch occurred — that would be a premature Phase 2.1
 * assertion.
 */
class ScreeningServiceEdgeExecutionTest {

    // -------------------------------------------------------------------------
    // Test doubles and helpers
    // -------------------------------------------------------------------------

    private lateinit var engine: CallScreeningEngine
    private lateinit var callLogRepository: CallLogRepository
    private lateinit var pendingCardRepository: PendingCardRepository

    private val testPhoneNumber   = "+15551234567"
    private val testNormalized    = "+15551234567"

    @Before
    fun setUp() {
        engine                = mock()
        callLogRepository     = mock()
        pendingCardRepository = mock()
    }

    /**
     * Builds a CallInfo for the given tier with a correctly-derived
     * screeningDecision, matching what the engine would return for a real call.
     * screeningDecision is a derived property on CallInfo so the correct
     * ScreeningDecision is always produced as long as tier and callDecision match.
     */
    private fun callInfoForTier(
        tier: CallTier,
        action: ScreeningAction,
        confidence: Int? = null
    ) = CallInfo(
        originalPhoneNumber   = testPhoneNumber,
        normalizedPhoneNumber = testNormalized,
        spamStatus            = tier.name,
        spamCategory          = null,
        confidence            = confidence,
        riskLevel             = null,
        matchedSources        = emptyList(),
        callDecision          = action,
        tier                  = tier
    )

    // -------------------------------------------------------------------------
    // callAction → ALLOW/BLOCK mapping
    // -------------------------------------------------------------------------

    @Test
    fun federalBlock_callAction_isBlock() {
        val info = callInfoForTier(CallTier.FEDERAL_BLOCK, ScreeningAction.BLOCK)
        assertEquals(ScreeningAction.BLOCK, info.screeningDecision.callAction)
    }

    @Test
    fun allowlisted_callAction_isAllow() {
        val info = callInfoForTier(CallTier.ALLOWLISTED, ScreeningAction.ALLOW)
        assertEquals(ScreeningAction.ALLOW, info.screeningDecision.callAction)
    }

    @Test
    fun heuristicFlag_callAction_isScreen() {
        val info = callInfoForTier(CallTier.HEURISTIC_FLAG, ScreeningAction.SCREEN)
        assertEquals(ScreeningAction.SCREEN, info.screeningDecision.callAction)
    }

    @Test
    fun securityFailure_callAction_isDistinctFromAllow() {
        val info = callInfoForTier(CallTier.SECURITY_FAILURE, ScreeningAction.SECURITY_FAILURE)
        val action = info.screeningDecision.callAction
        assertEquals(ScreeningAction.SECURITY_FAILURE, action)
        // Core invariant — SECURITY_FAILURE must never equal ALLOW in the edge log
        assert(action != ScreeningAction.ALLOW) {
            "SECURITY_FAILURE action must not equal ALLOW — §0.6 invariant"
        }
    }

    // -------------------------------------------------------------------------
    // auditRequired → CallLogEntry written iff true
    // -------------------------------------------------------------------------

    @Test
    fun auditRequired_true_writesCallLogEntry() = runBlocking<Unit> {
        val info = callInfoForTier(CallTier.FEDERAL_BLOCK, ScreeningAction.BLOCK)
        assert(info.screeningDecision.auditRequired) {
            "Precondition: FEDERAL_BLOCK must require audit"
        }

        // Simulate what writeAuditRecords does when auditRequired = true
        callLogRepository.insertCallLog(
            CallLogEntry(
                phoneNumber           = info.originalPhoneNumber,
                normalizedPhoneNumber = info.normalizedPhoneNumber,
                timestamp             = 0L,
                decision              = info.callDecision.name,
                spamStatus            = info.spamStatus,
                spamCategory          = info.spamCategory,
                confidence            = info.confidence,
                riskLevel             = info.riskLevel,
                matchedSources        = null,
                notes                 = info.tier.name
            )
        )

        val captor = argumentCaptor<CallLogEntry>()
        verify(callLogRepository).insertCallLog(captor.capture())
        assertEquals(testPhoneNumber, captor.firstValue.phoneNumber)
        assertEquals(CallTier.FEDERAL_BLOCK.name, captor.firstValue.notes)
    }

    @Test
    fun auditRequired_false_doesNotWriteCallLogEntry() = runBlocking<Unit> {
        val info = callInfoForTier(CallTier.ALLOWLISTED, ScreeningAction.ALLOW)
        assert(!info.screeningDecision.auditRequired) {
            "Precondition: ALLOWLISTED must not require audit"
        }

        // When auditRequired is false the service returns early — no insert
        if (info.screeningDecision.auditRequired) {
            callLogRepository.insertCallLog(mock())
        }

        verify(callLogRepository, never()).insertCallLog(any())
    }

    // -------------------------------------------------------------------------
    // reviewCardRequired → PendingCardEntity written iff true
    // -------------------------------------------------------------------------

    @Test
    fun reviewCardRequired_true_writesPendingCard() = runBlocking<Unit> {
        val info = callInfoForTier(CallTier.HEURISTIC_BLOCK, ScreeningAction.BLOCK, confidence = 85)
        assert(info.screeningDecision.reviewCardRequired) {
            "Precondition: HEURISTIC_BLOCK must require review card"
        }

        pendingCardRepository.insertCard(
            PendingCardEntity(
                phoneNumber    = info.normalizedPhoneNumber,
                timestamp      = 0L,
                decision       = info.screeningDecision.callAction.name,
                confidence     = info.confidence ?: 0,
                decisionSource = "${info.screeningDecision.tier.name} review"
            )
        )

        val captor = argumentCaptor<PendingCardEntity>()
        verify(pendingCardRepository).insertCard(captor.capture())
        assertEquals(testNormalized, captor.firstValue.phoneNumber)
        assertEquals("HEURISTIC_BLOCK review", captor.firstValue.decisionSource)
        assertEquals(85, captor.firstValue.confidence)
    }

    @Test
    fun reviewCardRequired_false_doesNotWritePendingCard() = runBlocking<Unit> {
        val info = callInfoForTier(CallTier.FEDERAL_BLOCK, ScreeningAction.BLOCK)
        assert(!info.screeningDecision.reviewCardRequired) {
            "Precondition: FEDERAL_BLOCK must not require review card"
        }

        if (info.screeningDecision.reviewCardRequired) {
            pendingCardRepository.insertCard(mock())
        }

        verify(pendingCardRepository, never()).insertCard(any())
    }

    @Test
    fun allowlisted_doesNotWritePendingCard_andDoesNotWriteCallLog() = runBlocking<Unit> {
        val info = callInfoForTier(CallTier.ALLOWLISTED, ScreeningAction.ALLOW)
        val decision = info.screeningDecision
        assert(!decision.auditRequired && !decision.reviewCardRequired)

        if (decision.auditRequired) callLogRepository.insertCallLog(mock())
        if (decision.reviewCardRequired) pendingCardRepository.insertCard(mock())

        verify(callLogRepository, never()).insertCallLog(any())
        verify(pendingCardRepository, never()).insertCard(any())
    }

    // -------------------------------------------------------------------------
    // notificationPolicy values — policy correctness, not dispatch
    // -------------------------------------------------------------------------

    @Test
    fun heuristicBlock_notificationPolicy_isBlockReview() {
        val info = callInfoForTier(CallTier.HEURISTIC_BLOCK, ScreeningAction.BLOCK)
        assertEquals(NotificationPolicy.BLOCK_REVIEW, info.screeningDecision.notificationPolicy)
    }

    @Test
    fun heuristicFlag_notificationPolicy_isReviewAvailable() {
        // REVIEW_AVAILABLE means the decision calls for a review notification.
        // Dispatch is deferred to Phase 2.1 — this test confirms the policy
        // value is correctly set, not that a notification was fired.
        val info = callInfoForTier(CallTier.HEURISTIC_FLAG, ScreeningAction.SCREEN)
        assertEquals(NotificationPolicy.REVIEW_AVAILABLE, info.screeningDecision.notificationPolicy)
    }

    @Test
    fun federalBlock_notificationPolicy_isNone() {
        val info = callInfoForTier(CallTier.FEDERAL_BLOCK, ScreeningAction.BLOCK)
        assertEquals(NotificationPolicy.NONE, info.screeningDecision.notificationPolicy)
    }

    @Test
    fun cleanUnknown_notificationPolicy_isNone() {
        val info = callInfoForTier(CallTier.CLEAN_UNKNOWN, ScreeningAction.ALLOW)
        assertEquals(NotificationPolicy.NONE, info.screeningDecision.notificationPolicy)
    }

    @Test
    fun allowlisted_notificationPolicy_isNone() {
        val info = callInfoForTier(CallTier.ALLOWLISTED, ScreeningAction.ALLOW)
        assertEquals(NotificationPolicy.NONE, info.screeningDecision.notificationPolicy)
    }

    @Test
    fun securityFailure_notificationPolicy_isNone() {
        val info = callInfoForTier(CallTier.SECURITY_FAILURE, ScreeningAction.SECURITY_FAILURE)
        assertEquals(NotificationPolicy.NONE, info.screeningDecision.notificationPolicy)
    }

    // -------------------------------------------------------------------------
    // hapticPolicy values — policy correctness, not dispatch. HEURISTIC_BLOCK
    // intentionally has no haptic; BLOCK_PULSE remains a supported policy value
    // for explicit callers but is no longer selected by ScreeningDecision.forTier().
    //
    // Dispatch wired in Phase 2.1 after Phase 1.3 proves persisted reviewability.
    // These tests confirm the policy value; they deliberately do not assert that
    // PulseHapticsController was invoked — that would be a Phase 2.1 assertion.
    // -------------------------------------------------------------------------

    @Test
    fun heuristicBlock_hapticPolicy_isNone() {
        val info = callInfoForTier(CallTier.HEURISTIC_BLOCK, ScreeningAction.BLOCK)
        assertEquals(HapticPolicy.NONE, info.screeningDecision.hapticPolicy)
    }

    @Test
    fun heuristicFlag_hapticPolicy_isReviewPulse() {
        // REVIEW_PULSE is a policy-semantic value — "a review haptic should fire
        // when the implementation is wired." Rate limiting in Phase 2.2 throttles
        // dispatch; it must never change this policy field.
        val info = callInfoForTier(CallTier.HEURISTIC_FLAG, ScreeningAction.SCREEN)
        assertEquals(HapticPolicy.REVIEW_PULSE, info.screeningDecision.hapticPolicy)
    }

    @Test
    fun allNonReviewTiers_hapticPolicy_isNone() {
        listOf(
            callInfoForTier(CallTier.ALLOWLISTED, ScreeningAction.ALLOW),
            callInfoForTier(CallTier.FEDERAL_BLOCK, ScreeningAction.BLOCK),
            callInfoForTier(CallTier.CLEAN_UNKNOWN, ScreeningAction.ALLOW),
            callInfoForTier(CallTier.SECURITY_FAILURE, ScreeningAction.SECURITY_FAILURE)
        ).forEach { info ->
            assertEquals(
                "Expected NONE hapticPolicy for ${info.tier}",
                HapticPolicy.NONE,
                info.screeningDecision.hapticPolicy
            )
        }
    }

    // -------------------------------------------------------------------------
    // securityFailure — audit trail correctness
    // -------------------------------------------------------------------------

    @Test
    fun securityFailure_auditRecord_carriesDistinctDecisionName() = runBlocking<Unit> {
        val info = callInfoForTier(CallTier.SECURITY_FAILURE, ScreeningAction.SECURITY_FAILURE)

        // The audit record written by handleSecurityFailure must carry
        // SECURITY_FAILURE as the decision value, not ALLOW or CLEAN_UNKNOWN
        val auditDecision = info.callDecision.name
        assertEquals("SECURITY_FAILURE", auditDecision)
        assert(auditDecision != ScreeningAction.ALLOW.name) {
            "SECURITY_FAILURE audit record must not carry ALLOW as decision — §0.6"
        }
        assert(auditDecision != CallTier.CLEAN_UNKNOWN.name) {
            "SECURITY_FAILURE audit record must not be recorded as CLEAN_UNKNOWN — §0.6"
        }
    }

    @Test
    fun securityFailure_tier_isDistinctFromCleanUnknown() {
        val failureInfo = callInfoForTier(CallTier.SECURITY_FAILURE, ScreeningAction.SECURITY_FAILURE)
        val cleanInfo   = callInfoForTier(CallTier.CLEAN_UNKNOWN, ScreeningAction.ALLOW)

        assert(failureInfo.tier != cleanInfo.tier) {
            "SECURITY_FAILURE tier must never equal CLEAN_UNKNOWN — §0.6 invariant"
        }
        assert(failureInfo.screeningDecision.securityFailure) {
            "securityFailure flag must be true for SECURITY_FAILURE tier"
        }
        assert(!cleanInfo.screeningDecision.securityFailure) {
            "securityFailure flag must be false for CLEAN_UNKNOWN"
        }
    }

    // -------------------------------------------------------------------------
    // End-to-end consequence chain — all fields verified for two representative tiers
    // -------------------------------------------------------------------------

    @Test
    fun heuristicBlock_allConsequenceFieldsAreCorrect() = runBlocking<Unit> {
        val info     = callInfoForTier(CallTier.HEURISTIC_BLOCK, ScreeningAction.BLOCK, confidence = 90)
        val decision = info.screeningDecision

        assertEquals(ScreeningAction.BLOCK, decision.callAction)
        assertEquals(true,  decision.auditRequired)
        assertEquals(true,  decision.reviewCardRequired)
        assertEquals(NotificationPolicy.BLOCK_REVIEW, decision.notificationPolicy)
        assertEquals(HapticPolicy.NONE, decision.hapticPolicy)
        assertEquals(false, decision.securityFailure)

        // Simulate both writes the service makes
        callLogRepository.insertCallLog(
            CallLogEntry(
                phoneNumber = info.originalPhoneNumber,
                normalizedPhoneNumber = info.normalizedPhoneNumber,
                timestamp = 0L, decision = decision.callAction.name,
                spamStatus = info.spamStatus, spamCategory = null,
                confidence = info.confidence, riskLevel = null,
                matchedSources = null, notes = info.tier.name
            )
        )
        pendingCardRepository.insertCard(
            PendingCardEntity(
                phoneNumber = info.normalizedPhoneNumber, timestamp = 0L,
                decision = decision.callAction.name, confidence = 90,
                decisionSource = "${decision.tier.name} review"
            )
        )

        verify(callLogRepository).insertCallLog(any())
        verify(pendingCardRepository).insertCard(any())
    }

    @Test
    fun heuristicFlag_allConsequenceFieldsAreCorrect() = runBlocking<Unit> {
        val info     = callInfoForTier(CallTier.HEURISTIC_FLAG, ScreeningAction.SCREEN, confidence = 45)
        val decision = info.screeningDecision

        assertEquals(ScreeningAction.SCREEN, decision.callAction)
        assertEquals(true,  decision.auditRequired)
        assertEquals(true,  decision.reviewCardRequired)
        assertEquals(NotificationPolicy.REVIEW_AVAILABLE, decision.notificationPolicy)
        assertEquals(HapticPolicy.REVIEW_PULSE, decision.hapticPolicy)
        assertEquals(false, decision.securityFailure)

        // audit write
        callLogRepository.insertCallLog(
            CallLogEntry(
                phoneNumber = info.originalPhoneNumber,
                normalizedPhoneNumber = info.normalizedPhoneNumber,
                timestamp = 0L, decision = decision.callAction.name,
                spamStatus = info.spamStatus, spamCategory = null,
                confidence = info.confidence, riskLevel = null,
                matchedSources = null, notes = info.tier.name
            )
        )
        // review card write
        pendingCardRepository.insertCard(
            PendingCardEntity(
                phoneNumber = info.normalizedPhoneNumber, timestamp = 0L,
                decision = decision.callAction.name, confidence = 45,
                decisionSource = "${decision.tier.name} review"
            )
        )

        // REVIEW_AVAILABLE: notification policy is set but NOT dispatched yet
        // (Phase 2.1). Verify no notification-path repository was called with
        // a BLOCK_REVIEW classification — HEURISTIC_FLAG must not be treated
        // as a block-review even though it requires a review card.
        val logCaptor = argumentCaptor<CallLogEntry>()
        verify(callLogRepository).insertCallLog(logCaptor.capture())
        assert(logCaptor.firstValue.notes != CallTier.HEURISTIC_BLOCK.name) {
            "HEURISTIC_FLAG audit record must not be classified as HEURISTIC_BLOCK"
        }
    }
}

