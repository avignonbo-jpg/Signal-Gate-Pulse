package com.signalgate.multipoint.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import timber.log.Timber

/**
 * NotificationChannelManager — Phase 4.8.
 *
 * Single registration point for every notification channel.
 * Called from MainApplication.onCreate() before any notification fires.
 * Re-registering an existing channel ID is a safe no-op — user settings preserved.
 *
 * BLOCKED_CALL_REVIEW: HIGH — Tier 3 heuristic blocks. User needs prompt visibility
 *   for false-positive recovery. Sound/vibration off (call just ended).
 * SYNC_STATUS: DEFAULT — WorkManager sync progress/completion. Also used as the
 *   foreground-service notification channel for Phase 4.9.
 * SECURITY_ALERT: HIGH — Role loss or critical permission revocation. Vibration on.
 *   Used by Phase 4.12 PermissionHealthCheckWorker (not yet built).
 */
object NotificationChannelManager {

    const val CHANNEL_BLOCKED_CALL_REVIEW = "blocked_call_review"
    const val CHANNEL_SYNC_STATUS         = "sync_status"
    const val CHANNEL_SECURITY_ALERT      = "security_alert"

    private const val TAG = "NotificationChannelMgr"

    fun createAllChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
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
