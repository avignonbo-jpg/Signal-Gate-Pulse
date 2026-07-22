package com.signalgate.multipoint.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * SyncBootReceiver — Phase 4.11.
 *
 * WorkManager persists its queue and auto-reschedules on boot, but some OEMs
 * (Samsung, Xiaomi) kill this path on Android 12+. This explicit receiver
 * guarantees CommunitySyncWorker is scheduled after reboot and after OTA update.
 *
 * MY_PACKAGE_REPLACED: the OS does NOT send BOOT_COMPLETED after an app update,
 * so both actions are required for full coverage.
 *
 * CommunitySyncWorker.schedule() uses ExistingPeriodicWorkPolicy.KEEP — safe to
 * call unconditionally, never creates duplicate work.
 */
class SyncBootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SyncBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        Timber.tag(TAG).i("$action received — rescheduling CommunitySyncWorker")
        CommunitySyncWorker.schedule(context)
    }
}
