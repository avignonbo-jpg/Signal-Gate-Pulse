package com.signalgate.pulse

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.signalgate.pulse.data.security.BloomFilterEngine
import com.signalgate.pulse.database.SignalGateDatabase
import com.signalgate.pulse.database.repositories.BlocklistRepository
import com.signalgate.pulse.database.repositories.CallLogRepository
import com.signalgate.pulse.database.repositories.DataSourceRepository
import com.signalgate.pulse.database.repositories.PendingCardRepository
import com.signalgate.pulse.database.repositories.SettingRepository
import com.signalgate.pulse.logic.ScreeningAction
import com.signalgate.pulse.logic.SecurityRuleRepository
import com.signalgate.pulse.ui.digest.DigestScreen
import com.signalgate.pulse.ui.notifications.PulseTriggerLimiter
import com.signalgate.pulse.ui.digest.PendingCardViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 1.3 — complete gray-zone reviewability proof.
 *
 * This test intentionally crosses every required boundary:
 * decision -> audit record -> PendingCardEntity -> repository Flow
 * -> PendingCardViewModel -> DigestScreen.
 *
 * It calls the service's internal persistence seam rather than duplicating the
 * writes in the test. That proves the edge executes reviewCardRequired for
 * HEURISTIC_FLAG, not merely that the repositories can store a hand-built row.
 */
@RunWith(AndroidJUnit4::class)
class GrayZoneReviewabilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: SignalGateDatabase
    private lateinit var callLogRepository: CallLogRepository
    private lateinit var pendingCardRepository: PendingCardRepository
    private lateinit var viewModel: PendingCardViewModel

    private val phoneNumber = "+15551234567"

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, SignalGateDatabase::class.java).build()

        val dataSourceRepository = DataSourceRepository(
            database.sourceDao(),
            database.unifiedEntryDao(),
            BloomFilterEngine(),
            BloomFilterEngine()
        )
        val securityRuleRepository = SecurityRuleRepository(
            dataSourceRepository = dataSourceRepository,
            database = database,
            unifiedEntryDao = database.unifiedEntryDao(),
            settingRepository = SettingRepository(database.settingDao())
        )

        callLogRepository = CallLogRepository(database.callLogDao())
        pendingCardRepository = PendingCardRepository(database.pendingCardDao())
        viewModel = PendingCardViewModel(
            pendingCardRepository = pendingCardRepository,
            blocklistRepository = BlocklistRepository(securityRuleRepository)
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun rateLimitedReviewUx_doesNotSuppressAuditOrReviewCardPersistence() = runBlocking<Unit> {
        val callInfo = CallInfo(
            originalPhoneNumber = phoneNumber,
            normalizedPhoneNumber = phoneNumber,
            spamStatus = CallTier.HEURISTIC_FLAG.name,
            spamCategory = "advisory",
            confidence = 45,
            riskLevel = "MEDIUM",
            matchedSources = listOf("advisory-pattern"),
            callDecision = ScreeningAction.SCREEN,
            tier = CallTier.HEURISTIC_FLAG
        )
        val decision = callInfo.screeningDecision
        val limiter = PulseTriggerLimiter()

        // The service persists consequences before consulting the limiter. Both
        // calls must therefore retain their audit and review-card records even
        // though the second notification/haptic dispatch is suppressed.
        SignalGateCallScreeningService().executeDecisionConsequences(
            callInfo = callInfo,
            decision = decision,
            callLogRepository = callLogRepository,
            pendingCardRepository = pendingCardRepository,
            now = { 1_700_000_000_000L }
        )
        val firstDispatch = limiter.shouldDispatchNotification(
            decision.notificationPolicy,
            phoneNumber,
            now = 1_700_000_000_000L
        )

        SignalGateCallScreeningService().executeDecisionConsequences(
            callInfo = callInfo,
            decision = decision,
            callLogRepository = callLogRepository,
            pendingCardRepository = pendingCardRepository,
            now = { 1_700_000_000_001L }
        )
        val secondDispatch = limiter.shouldDispatchNotification(
            decision.notificationPolicy,
            phoneNumber,
            now = 1_700_000_000_001L
        )

        assertTrue("The first review UX dispatch should be allowed", firstDispatch)
        assertFalse("The repeated review UX dispatch should be throttled", secondDispatch)
        assertEquals("Rate limiting must not suppress audit records", 2, callLogRepository.getCallsByPhoneNumber(phoneNumber).size)
        assertEquals("Rate limiting must not suppress required review cards", 2, pendingCardRepository.getUndismissedCards().first().size)
    }

    @Test
    fun heuristicFlag_flowsFromDecisionThroughPersistenceViewModelAndDigest() = runBlocking<Unit> {
        val callInfo = CallInfo(
            originalPhoneNumber = phoneNumber,
            normalizedPhoneNumber = phoneNumber,
            spamStatus = CallTier.HEURISTIC_FLAG.name,
            spamCategory = "advisory",
            confidence = 45,
            riskLevel = "MEDIUM",
            matchedSources = listOf("advisory-pattern"),
            callDecision = ScreeningAction.SCREEN,
            tier = CallTier.HEURISTIC_FLAG
        )
        val decision = callInfo.screeningDecision

        assertEquals(ScreeningAction.SCREEN, decision.callAction)
        assertTrue("HEURISTIC_FLAG must require an audit", decision.auditRequired)
        assertTrue("HEURISTIC_FLAG must require a review card", decision.reviewCardRequired)

        SignalGateCallScreeningService().executeDecisionConsequences(
            callInfo = callInfo,
            decision = decision,
            callLogRepository = callLogRepository,
            pendingCardRepository = pendingCardRepository,
            now = { 1_700_000_000_000L }
        )

        val auditRecords = callLogRepository.getCallsByPhoneNumber(phoneNumber)
        assertEquals("Exactly one gray-zone audit record is required", 1, auditRecords.size)
        assertEquals(ScreeningAction.SCREEN.name, auditRecords.single().decision)
        assertEquals(CallTier.HEURISTIC_FLAG.name, auditRecords.single().notes)

        val persistedCards = pendingCardRepository.getUndismissedCards().first()
        assertEquals("Exactly one undismissed gray-zone card is required", 1, persistedCards.size)
        val card = persistedCards.single()
        assertEquals(phoneNumber, card.phoneNumber)
        assertEquals(ScreeningAction.SCREEN.name, card.decision)
        assertEquals("HEURISTIC_FLAG review", card.decisionSource)
        assertEquals(45, card.confidence)
        assertFalse(card.dismissed)
        assertNotNull("The repository must assign a card ID", card.id.takeIf { it > 0 })

        val viewModelCards = viewModel.undismissedCards.first()
        assertEquals("DigestViewModel must expose the persisted card", persistedCards, viewModelCards)
        assertEquals(1, viewModel.undismissedCount.first())

        composeRule.setContent {
            DigestScreen(viewModel = viewModel)
        }
        composeRule.onNodeWithText(phoneNumber).assertIsDisplayed()
        composeRule.onNodeWithText("Matched: HEURISTIC_FLAG review").assertIsDisplayed()
        composeRule.onNodeWithText("45%").assertIsDisplayed()
        composeRule.onNodeWithText("1 awaiting review").assertIsDisplayed()
    }
}
