package com.signalgate.multipoint.ui.onboarding

import android.Manifest
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PermissionItem(
    val permission: String,
    val title: String,
    val rationale: String,
    val isRequired: Boolean,
    val isGranted: Boolean = false
)

class OnboardingViewModel : ViewModel() {

    val permissions = listOf(
        PermissionItem(
            permission = Manifest.permission.READ_PHONE_STATE,
            title = "Phone State",
            rationale = "Lets SignalGate detect when a call is incoming so it can screen it before your phone rings.",
            isRequired = true
        ),
        PermissionItem(
            permission = Manifest.permission.READ_PHONE_NUMBERS,
            title = "Phone Number",
            rationale = "Reads your own number so SignalGate can exclude it from screening.",
            isRequired = true
        ),
        PermissionItem(
            permission = Manifest.permission.READ_CALL_LOG,
            title = "Call Log",
            rationale = "Shows your recent call history in the dashboard so you can review screened calls.",
            isRequired = false
        ),
        PermissionItem(
            permission = Manifest.permission.READ_CONTACTS,
            title = "Contacts",
            rationale = "Lets SignalGate automatically allow calls from people in your contacts list.",
            isRequired = false
        ),
        PermissionItem(
            permission = Manifest.permission.POST_NOTIFICATIONS,
            title = "Notifications",
            rationale = "Sends you an alert when a call is screened or blocked.",
            isRequired = true
        )
    )

    private val _permissionStates = MutableStateFlow(
        permissions.associate { it.permission to false }
    )
    val permissionStates = _permissionStates.asStateFlow()

    var riskThreshold: String = "Medium"

    fun onPermissionResult(permission: String, granted: Boolean) {
        _permissionStates.value = _permissionStates.value.toMutableMap().apply {
            put(permission, granted)
        }
    }

    fun allRequiredGranted(): Boolean {
        return permissions
            .filter { it.isRequired }
            .all { _permissionStates.value[it.permission] == true }
    }
}
