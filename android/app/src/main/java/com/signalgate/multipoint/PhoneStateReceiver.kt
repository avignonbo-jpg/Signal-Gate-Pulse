package com.signalgate.multipoint

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import timber.log.Timber

/**
 * PhoneStateReceiver — INTENTIONALLY INERT (2026-06).
 *
 * This receiver previously fired PostCallNotifier.show() on every call ending,
 * via a SharedPreferences key ("LAST_CALL_NUMBER") that nothing in the app
 * ever wrote. That made it a dead landmine: harmless only because the key was
 * never populated, but any future code path that wrote that key would have
 * reactivated a parallel, completely untiered "Block or whitelist?" popup on
 * every single call — directly conflicting with SignalGateCallScreeningService's
 * Tier 3 notification system (Step 1.6–1.8).
 *
 * PostCallNotifier.kt has been deleted. This receiver is retained as a no-op
 * because:
 *   1. It is registered exported=true with READ_PHONE_STATE permission in the
 *      manifest — removing the manifest entry changes the app's declared
 *      attack surface and should be a deliberate manifest-review decision,
 *      not a side effect of a dead-code cleanup.
 *   2. Keeping it as a documented no-op makes the historical landmine
 *      impossible to accidentally reactivate, while leaving the manifest
 *      surface unchanged for review.
 *
 * If a future feature genuinely needs to react to call-state transitions
 * outside of CallScreeningService (none currently do — the five-tier system
 * owns all post-call notification logic), implement it here explicitly and
 * remove this comment. Do not restore PostCallNotifier or any SharedPreferences
 * "LAST_CALL_NUMBER" bridge — route through CallScreeningService / PendingCardDao
 * instead, consistent with the rest of the audit trail.
 */
class PhoneStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PhoneStateReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        Timber.tag(TAG).d("Phone state changed: $state (no-op — see class doc)")
        // Intentionally no further action. See class-level documentation.
    }
}
