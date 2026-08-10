package com.signalgate.multipoint.ui.onboarding

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signalgate.multipoint.database.repositories.HeuristicsMode
import com.signalgate.multipoint.database.repositories.SettingKeys
import com.signalgate.multipoint.database.repositories.SettingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

data class PermissionItem(
    val permission: String,
    val title: String,
    val description: String,
    val rationale: String,
    val isRequired: Boolean,
    val isGranted: Boolean = false
)

class OnboardingViewModel(
    private val settingRepository: SettingRepository
) : ViewModel() {

    companion object {
        private const val TAG = "OnboardingViewModel"
    }

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

    /**
     * Step 3 protection level. Replaces the old dead `riskThreshold: String`
     * field (never read or persisted anywhere) with a real, persisted setting.
     * Defaults to BALANCED and is written to SettingEntry as soon as the user
     * taps a level, not deferred to the final "GO TO DASHBOARD" tap — so it
     * takes effect even if they background the app mid-wizard.
     */
    private val _heuristicsMode = MutableStateFlow(HeuristicsMode.DEFAULT)
    val heuristicsMode = _heuristicsMode.asStateFlow()

    fun setHeuristicsMode(mode: HeuristicsMode) {
        _heuristicsMode.value = mode
        viewModelScope.launch {
            try {
                settingRepository.setSetting(SettingKeys.HEURISTICS_MODE, mode.key)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to persist heuristics_mode")
            }
        }
    }

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
     * Onboarding-completion state, migrated off SharedPreferences under Step 2.6.
     *
     * Follows the same screen-observes-ViewModel-state pattern ContactsViewModel
     * already uses for isSaved: the Composable calls markOnboardingComplete() and
     * observes this flow via LaunchedEffect to trigger navigation once persistence
     * actually succeeds — it never touches SettingRepository directly, and never
     * needs its own coroutine scope to do so.
     */
    private val _onboardingCompleted = MutableStateFlow(false)
    val onboardingCompleted = _onboardingCompleted.asStateFlow()

    fun markOnboardingComplete() {
        viewModelScope.launch {
            try {
                settingRepository.setSetting(SettingKeys.ONBOARDING_COMPLETE, "true")
                _onboardingCompleted.value = true
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to persist onboarding_complete")
            }
        }
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
