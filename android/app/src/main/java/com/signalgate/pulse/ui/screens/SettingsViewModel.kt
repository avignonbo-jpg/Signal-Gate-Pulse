package com.signalgate.multipoint.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signalgate.multipoint.database.repositories.SettingKeys
import com.signalgate.multipoint.database.repositories.SettingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * SettingsViewModel — owns shield-color persistence for SettingsScreen.
 *
 * Created under Step 2.6 specifically to migrate shield_red/green/blue off
 * SharedPreferences onto SettingEntry via SettingRepository. As a byproduct,
 * this also resolves half of Phase 0's FLAG-1 ("SettingsScreen has no owning
 * ViewModel") — the other half (TelemetryViewModel's orphan-screen problem)
 * is a separate, already-scoped rename/relocate PR, not addressed here.
 *
 * UI -> ViewModel -> SettingRepository -> DAO: SettingsScreen never touches
 * SharedPreferences, SettingRepository, or SettingDao directly.
 */
class SettingsViewModel(
    private val settingRepository: SettingRepository
) : ViewModel() {

    companion object {
        // Matches the defaults SettingsScreen previously read from
        // SharedPreferences.getInt(key, default) — neon cyan.
        private const val DEFAULT_RED = 66
        private const val DEFAULT_GREEN = 133
        private const val DEFAULT_BLUE = 244
    }

    private val _shieldRed = MutableStateFlow(DEFAULT_RED)
    val shieldRed = _shieldRed.asStateFlow()

    private val _shieldGreen = MutableStateFlow(DEFAULT_GREEN)
    val shieldGreen = _shieldGreen.asStateFlow()

    private val _shieldBlue = MutableStateFlow(DEFAULT_BLUE)
    val shieldBlue = _shieldBlue.asStateFlow()

    private val _saveConfirmed = MutableStateFlow(false)
    val saveConfirmed = _saveConfirmed.asStateFlow()

    init {
        loadShieldColor()
    }

    private fun loadShieldColor() {
        viewModelScope.launch {
            _shieldRed.value = settingRepository.getSettingValue(SettingKeys.SHIELD_RED)
                ?.toIntOrNull() ?: DEFAULT_RED
            _shieldGreen.value = settingRepository.getSettingValue(SettingKeys.SHIELD_GREEN)
                ?.toIntOrNull() ?: DEFAULT_GREEN
            _shieldBlue.value = settingRepository.getSettingValue(SettingKeys.SHIELD_BLUE)
                ?.toIntOrNull() ?: DEFAULT_BLUE
        }
    }

    /**
     * Live slider position, held here rather than as Compose-local state so
     * SettingsScreen never needs its own mutableFloatStateOf — the screen only
     * ever reads these three flows and calls onSliderChange/saveShieldColor.
     */
    fun onSliderChange(red: Int, green: Int, blue: Int) {
        _shieldRed.value = red
        _shieldGreen.value = green
        _shieldBlue.value = blue
    }

    fun saveShieldColor() {
        viewModelScope.launch {
            settingRepository.setSetting(SettingKeys.SHIELD_RED, _shieldRed.value.toString())
            settingRepository.setSetting(SettingKeys.SHIELD_GREEN, _shieldGreen.value.toString())
            settingRepository.setSetting(SettingKeys.SHIELD_BLUE, _shieldBlue.value.toString())
            _saveConfirmed.value = true
        }
    }

    /** Called by the screen once it's shown the save-confirmation dialog. */
    fun acknowledgeSave() {
        _saveConfirmed.value = false
    }
}
