package com.signalgate.pulse

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.signalgate.pulse.database.repositories.PendingCardRepository
import com.signalgate.pulse.logic.SecurityRuleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Handles notification action buttons fired from CallScreeningService's
 * blocked-call notification (Step 1.8).
 *
 * ACTION_NOT_SPAM is the only action. Five legacy actions were removed here
 * (ACTION_BLOCK_PERMANENT, ACTION_WHITELIST, ACTION_BLOCK_PREFIX,
 * ACTION_BLOCK_AREA_CODE, ACTION_IGNORE) — confirmed dead code with zero live
 * callers, and two of them (PREFIX/AREA_CODE) carried a confidence-threshold
 * bug: they inserted pattern-based UnifiedEntryEntity rows with no confidence
 * set, which defaults to 0 — below CallScreeningEngine's 70% high-confidence
 * threshold, so the resulting block would have tiered as HEURISTIC_FLAG
 * (rings through) instead of HEURISTIC_BLOCK (silenced), silently failing to
 * honor the user's explicit block choice.
 *
 * Phase 0.1 (Security Control-Plane Integrity, Known Violation §11.10): this
 * receiver previously injected PendingCardDao directly, violating Layer 1's
 * "no direct DAO access" rule (Architecture Contract §2/§7) — a
 * BroadcastReceiver is an ingress point, not a persistence owner. It now
 * depends on PendingCardRepository (the existing Layer 3 wrapper — already
 * present in the codebase, just not used here before) and
 * SecurityRuleRepository (the new Layer 5 mutation boundary, §5.2) instead
 * of BlocklistRepository, since BlocklistRepository is itself now a
 * deprecated facade over SecurityRuleRepository (see that class's doc).
 */
class CallActionReceiver : BroadcastReceiver(), KoinComponent {

    companion object {
        const val ACTION_NOT_SPAM       = "ACTION_NOT_SPAM"
        const val EXTRA_PHONE_NUMBER    = "PHONE_NUMBER"
        const val EXTRA_NOTIFICATION_ID = "NOTIFICATION_ID"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val securityRuleRepository: SecurityRuleRepository by inject()
    private val pendingCardRepository: PendingCardRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER)
        val action = intent.action
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        scope.launch {
            handleAction(context, phoneNumber, action, notificationId)
        }
    }

    /**
     * Same-file test seam for the receiver's behavioral gate. This is deliberately
     * not a new application service or interface: CallActionReceiver remains the
     * Layer 1 ingress owner, while the delegate makes its existing validation and
     * repository calls testable without Koin or an Android runner.
     */
    internal suspend fun handleAction(
        context: Context,
        phoneNumber: String?,
        action: String?,
        notificationId: Int = -1,
        securityRules: SecurityRuleRepository = securityRuleRepository,
        pendingCards: PendingCardRepository = pendingCardRepository,
        toast: (Context, String) -> Unit = ::showToast
    ) {
        if (phoneNumber == null || action != ACTION_NOT_SPAM) return

        // Allowlist the number via the single mutation boundary (§5.2)
        securityRules.addManualAllow(phoneNumber, "Not Spam — user overturn")
        // Dismiss any undismissed cards for this number in the digest queue
        pendingCards.dismissByPhoneNumber(phoneNumber)
        // Cancel the notification so it disappears immediately
        if (notificationId != -1) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            nm.cancel(notificationId)
        }
        toast(context, "Number added to allow list")
    }

    private fun showToast(context: Context, message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
