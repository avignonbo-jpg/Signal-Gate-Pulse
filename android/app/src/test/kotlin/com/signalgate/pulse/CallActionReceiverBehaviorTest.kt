package com.signalgate.pulse

import android.content.Context
import com.signalgate.pulse.database.repositories.PendingCardRepository
import com.signalgate.pulse.logic.SecurityRuleRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

/**
 * Phase 0.7 / 0.8 regression coverage.
 *
 * CallActionReceiver must reject incomplete or unrelated broadcasts before any
 * decision-affecting repository operation, while the supported action must
 * route allowlisting and digest dismissal through their application boundaries.
 */
class CallActionReceiverBehaviorTest {

    @Test
    fun missingPhoneNumber_isRejectedBeforeRepositoriesAreTouched() = runBlocking {
        val securityRules = mock<SecurityRuleRepository>()
        val pendingCards = mock<PendingCardRepository>()

        receiver().handleAction(
            context = mock(),
            phoneNumber = null,
            action = CallActionReceiver.ACTION_NOT_SPAM,
            securityRules = securityRules,
            pendingCards = pendingCards,
            toast = noOpToast
        )

        verifyNoInteractions(securityRules, pendingCards)
    }

    @Test
    fun missingAction_isRejectedBeforeRepositoriesAreTouched() = runBlocking {
        val securityRules = mock<SecurityRuleRepository>()
        val pendingCards = mock<PendingCardRepository>()

        receiver().handleAction(
            context = mock(),
            phoneNumber = "+15551234567",
            action = null,
            securityRules = securityRules,
            pendingCards = pendingCards,
            toast = noOpToast
        )

        verifyNoInteractions(securityRules, pendingCards)
    }

    @Test
    fun unrelatedAction_isRejectedBeforeRepositoriesAreTouched() = runBlocking {
        val securityRules = mock<SecurityRuleRepository>()
        val pendingCards = mock<PendingCardRepository>()

        receiver().handleAction(
            context = mock(),
            phoneNumber = "+15551234567",
            action = "UNRELATED_ACTION",
            securityRules = securityRules,
            pendingCards = pendingCards,
            toast = noOpToast
        )

        verifyNoInteractions(securityRules, pendingCards)
    }

    @Test
    fun notSpamAction_allowlistsAndDismissesThroughApplicationBoundaries() = runBlocking {
        val securityRules = mock<SecurityRuleRepository>()
        val pendingCards = mock<PendingCardRepository>()
        val messages = mutableListOf<String>()
        val phoneNumber = "+15551234567"

        receiver().handleAction(
            context = mock<Context>(),
            phoneNumber = phoneNumber,
            action = CallActionReceiver.ACTION_NOT_SPAM,
            securityRules = securityRules,
            pendingCards = pendingCards,
            toast = { _, message -> messages += message }
        )

        verify(securityRules).addManualAllow(phoneNumber, "Not Spam — user overturn")
        verify(pendingCards).dismissByPhoneNumber(phoneNumber)
        assertEquals(listOf("Number added to allow list"), messages)
    }

    private fun receiver(): CallActionReceiver = CallActionReceiver()

    private val noOpToast: (Context, String) -> Unit = { _, _ -> }
}
