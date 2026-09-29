package com.signalgate.pulse.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import timber.log.Timber

/**
 * NotificationChannelManager — Phase 4.8.
 *
 * Central channel registry, called from MainApplication.onCreate() during startup.
 * SignalGateCallScreeningService also re-registers BLOCKED_CALL_REVIEW immediately
 * before posting; keep that duplicate definition aligned with this one.
 * Re-registering an existing channel ID is a safe no-op — user settings preserved.
 *
 * BLOCKED_CALL_REVIEW: HIGH — Tier 3 heuristic blocks. User needs prompt visibility
 *   for false-positive recovery. Sound/vibration off (call just ended).
 * SYNC_STATUS: DEFAULT — Reserved for possible future background-sync
 *   progress/completion notifications. The current CommunitySyncWorker does not post
 *   notifications or use foreground-work execution.
 * SECURITY_ALERT: HIGH — Reserved for possible future alerts about call-screening
 *   role loss or critical permission revocation. No worker or notification-posting
 *   path currently uses this channel.
 */
object NotificationChannelManager {

    const val CHANNEL_BLOCKED_CALL_REVIEW = "blocked_call_review"
    const val CHANNEL_SYNC_STATUS         = "sync_status"
    const val CHANNEL_SECURITY_ALERT      = "security_alert"

    private const val TAG = "NotificationChannelMgr"

    fun createAllChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannels(listOf(
            blockedCallReviewChannel(),
            syncStatusChannel(),
            securityAlertChannel()
        ))
        Timber.tag(TAG).d("All notification channels registered")
    }

    private fun blockedCallReviewChannel() = NotificationChannel(
        CHANNEL_BLOCKED_CALL_REVIEW, "Blocked Call Review", NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "Review calls blocked by SignalGate Pulse"
        setShowBadge(true); enableVibration(false); setSound(null, null)
    }

    private fun syncStatusChannel() = NotificationChannel(
        CHANNEL_SYNC_STATUS, "Sync Status", NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = "Background sync progress for federal blocklist sources"
        setShowBadge(false); enableVibration(false); setSound(null, null)
    }

    private fun securityAlertChannel() = NotificationChannel(
        CHANNEL_SECURITY_ALERT, "Security Alert", NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "Critical alerts — call screening role lost or permission revoked"
        setShowBadge(true); enableVibration(true)
    }
}
