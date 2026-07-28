package com.signalgate.multipoint.ui.onboarding

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PermissionItem(
    val permission: String,
    val title: String,
    val description: String,
    val rationale: String,
    val isRequired: Boolean,
    val isGranted: Boolean = false
)

class OnboardingViewModel : ViewModel() {

    val permissions = mutableListOf(
        PermissionItem(
            permission = Manifest.permission.READ_PHONE_STATE,
            title = "Phone Connection",
            description = "Detect incoming calls for screening.",
            rationale = "SignalGate needs to know when your phone is receiving a call so it can analyze the signal before it reaches you.",
            isRequired = true
        ),
        PermissionItem(
            permission = Manifest.permission.READ_PHONE_NUMBERS,
            title = "Identity Verification",
            description = "Verify your own number for routing.",
            rationale = "We need to verify your identity to ensure the call screening service is correctly configured for your specific line.",
            isRequired = true
        ),
        PermissionItem(
            permission = Manifest.permission.ANSWER_PHONE_CALLS,
            title = "Call Control",
            description = "Allow the app to manage calls.",
            rationale = "This allows SignalGate to automatically handle calls based on your security settings, saving you from manual effort.",
            isRequired = true
        ),
        PermissionItem(
            permission = Manifest.permission.READ_CALL_LOG,
            title = "Call History",
            description = "Show history of screened calls.",
            rationale = "Accessing your call log allows you to see exactly which calls were blocked or allowed in your dashboard history.",
            isRequired = false
        ),
        PermissionItem(
            permission = Manifest.permission.WRITE_CALL_LOG,
            title = "Log Management",
            description = "Clean up spam from your history.",
            rationale = "This allows SignalGate to mark or remove identified spam calls from your history, keeping your log clean.",
            isRequired = false
        ),
        PermissionItem(
            permission = Manifest.permission.READ_CONTACTS,
            title = "Contacts Access",
            description = "Automatically allow known contacts.",
            rationale = "By reading your contacts, SignalGate can instantly recognize your friends and family, ensuring they never get blocked.",
            isRequired = false
        )
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(
                PermissionItem(
                    permission = Manifest.permission.POST_NOTIFICATIONS,
                    title = "Security Alerts",
                    description = "Get notified about blocked threats.",
                    rationale = "Enable notifications so we can alert you in real-time when a suspicious call is intercepted.",
                    isRequired = true
                )
            )
        }
    }.toList()

    private val _permissionStates = MutableStateFlow(
        permissions.associate { it.permission to false }
    )
    val permissionStates = _permissionStates.asStateFlow()

    // ROLE_CALL_SCREENING state — checked on every ON_RESUME, never cached between resumes
    private val _callScreeningRoleHeld = MutableStateFlow(false)
    val callScreeningRoleHeld = _callScreeningRoleHeld.asStateFlow()

    var riskThreshold: String = "Medium"

    fun onPermissionResult(permission: String, granted: Boolean) {
        _permissionStates.value = _permissionStates.value.toMutableMap().apply {
            put(permission, granted)
        }
    }

    fun updateAllPermissions(states: Map<String, Boolean>) {
        _permissionStates.value = states
    }

    fun allRequiredGranted(): Boolean {
        return permissions
            .filter { it.isRequired }
            .all { _permissionStates.value[it.permission] == true }
    }

    /**
     * Check whether ROLE_CALL_SCREENING is currently held.
     * Must be called on every ON_RESUME — never rely on a cached value.
     * Only available on API 29+; returns false on older devices.
     */
    fun checkCallScreeningRole(context: Context) {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        _callScreeningRoleHeld.value = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }
}
