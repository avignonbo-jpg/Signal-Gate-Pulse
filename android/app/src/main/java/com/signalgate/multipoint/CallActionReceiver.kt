package com.signalgate.multipoint

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.signalgate.multipoint.database.daos.PendingCardDao
import com.signalgate.multipoint.database.entities.UnifiedEntryEntity
import com.signalgate.multipoint.database.repositories.BlocklistRepository
import com.signalgate.multipoint.database.repositories.DataSourceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CallActionReceiver : BroadcastReceiver(), KoinComponent {

    companion object {
        const val ACTION_NOT_SPAM       = "ACTION_NOT_SPAM"
        const val EXTRA_PHONE_NUMBER    = "PHONE_NUMBER"
        const val EXTRA_NOTIFICATION_ID = "NOTIFICATION_ID"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val repository: DataSourceRepository by inject()
    private val blocklistRepository: BlocklistRepository by inject()
    private val pendingCardDao: PendingCardDao by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: return
        val action = intent.action ?: return

        scope.launch {
            when (action) {
                ACTION_NOT_SPAM -> {
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

                // PULSE-TODO (2026-06-28): Actions below use hardcoded sourceId = 1.
                // Migrate to SettingEntry-backed manual_source_id in Step 2.6.
                "ACTION_BLOCK_PERMANENT" -> {
                    repository.insertEntry(
                        UnifiedEntryEntity(
                            phoneNumber = phoneNumber,
                            action = "BLOCK",
                            sourceId = 1
                        )
                    )
                    showToast(context, "Number blocked permanently")
                }
                "ACTION_WHITELIST" -> {
                    repository.insertEntry(
                        UnifiedEntryEntity(
                            phoneNumber = phoneNumber,
                            action = "ALLOW",
                            sourceId = 1
                        )
                    )
                    showToast(context, "Number added to whitelist")
                }
                "ACTION_BLOCK_PREFIX" -> {
                    repository.insertEntry(
                        UnifiedEntryEntity(
                            phoneNumber = phoneNumber,
                            action = "BLOCK",
                            sourceId = 1,
                            isPattern = true
                        )
                    )
                    showToast(context, "Prefix blocked")
                }
                "ACTION_BLOCK_AREA_CODE" -> {
                    val areaCode = phoneNumber.take(4)
                    repository.insertEntry(
                        UnifiedEntryEntity(
                            phoneNumber = areaCode,
                            action = "BLOCK",
                            sourceId = 1,
                            isPattern = true
                        )
                    )
                    showToast(context, "Area code blocked")
                }
                "ACTION_IGNORE" -> {
                    showToast(context, "Call ignored")
                }
            }
        }
    }

    private fun showToast(context: Context, message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
