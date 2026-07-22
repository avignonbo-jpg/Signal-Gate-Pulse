package com.signalgate.multipoint

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.signalgate.multipoint.database.daos.PendingCardDao
import com.signalgate.multipoint.database.repositories.BlocklistRepository
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
 * honor the user's explicit block choice. BlocklistRepository.addBlockRule()
 * already does this correctly (confidence=100, proper MANUAL sourceId) — use
 * that going forward for any future manual block/allow UI.
 */
class CallActionReceiver : BroadcastReceiver(), KoinComponent {

    companion object {
        const val ACTION_NOT_SPAM       = "ACTION_NOT_SPAM"
        const val EXTRA_PHONE_NUMBER    = "PHONE_NUMBER"
        const val EXTRA_NOTIFICATION_ID = "NOTIFICATION_ID"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val blocklistRepository: BlocklistRepository by inject()
    private val pendingCardDao: PendingCardDao by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: return
        val action = intent.action ?: return

        if (action != ACTION_NOT_SPAM) return

        scope.launch {
            // Allowlist the number via BlocklistRepository (uses correct MANUAL sourceId)
            blocklistRepository.addAllowRule(phoneNumber, "Not Spam — user overturn")
            // Dismiss any undismissed cards for this number in the digest queue
            pendingCardDao.dismissByPhoneNumber(phoneNumber)
            // Cancel the notification so it disappears immediately
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
            if (notificationId != -1) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager
                nm.cancel(notificationId)
            }
            showToast(context, "Number added to allow list")
        }
    }

    private fun showToast(context: Context, message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
