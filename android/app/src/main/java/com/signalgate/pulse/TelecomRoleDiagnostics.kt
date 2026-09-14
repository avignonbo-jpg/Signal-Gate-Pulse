package com.signalgate.pulse

import android.app.role.RoleManager
import android.content.Context
import android.util.Log

/**
 * Read-only diagnostics for Android Telecom role selection.
 *
 * This does not request, grant, or change any role. It only records the
 * RoleManager state that determines whether Telecom can select SignalGate
 * as the user's call-screening application.
 *
 * The output contains package/class identifiers only. It never records
 * phone numbers, call details, contacts, or other call-specific data.
 */
object TelecomRoleDiagnostics {
    private const val TAG = "SignalGate"
    private const val ROLE = RoleManager.ROLE_CALL_SCREENING

    fun log(context: Context) {
        val roleManager = context.getSystemService(RoleManager::class.java)

        if (roleManager == null) {
            Log.w(TAG, "TELECOM_ROLE_DIAGNOSTIC: role_manager_unavailable")
            return
        }

        try {
            val available = roleManager.isRoleAvailable(ROLE)
            val held = roleManager.isRoleHeld(ROLE)

            Log.i(TAG, "TELECOM_ROLE_DIAGNOSTIC: role=$ROLE")
            Log.i(TAG, "TELECOM_ROLE_DIAGNOSTIC: role_available=$available")
            Log.i(TAG, "TELECOM_ROLE_DIAGNOSTIC: role_held_by_this_app=$held")
            Log.i(TAG, "TELECOM_ROLE_DIAGNOSTIC: package=${context.packageName}")
            Log.i(
                TAG,
                "TELECOM_ROLE_DIAGNOSTIC: service=com.signalgate.pulse.SignalGateCallScreeningService"
            )
        } catch (e: Exception) {
            Log.e(TAG, "TELECOM_ROLE_DIAGNOSTIC: role_query_failed", e)
        }
    }
}
