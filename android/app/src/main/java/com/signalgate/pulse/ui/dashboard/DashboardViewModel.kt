package com.signalgate.pulse.ui.dashboard

import android.app.role.RoleManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signalgate.pulse.database.entities.SourceEntity
import com.signalgate.pulse.database.repositories.CallLogRepository
import com.signalgate.pulse.database.repositories.DataSourceRepository
import com.signalgate.pulse.database.repositories.SettingKeys
import com.signalgate.pulse.database.repositories.SettingRepository
import com.signalgate.pulse.logic.SourceSyncUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar

/**
 * DashboardViewModel — Main dashboard state management.
 *
 * Responsibilities:
 * - Expose data source LED states (enabled/disabled per source)
 * - Track total sources and entry counts
 * - Manage shield status (ROLE_CALL_SCREENING granted?)
 * - Track calls screened today and blocked today
 * - Coordinate source syncs via repository
 *
 * Step 0.1 (2026-07-02): refreshCounters() now wired to CallLogDao queries.
 * Fetches blocked and screened call counts for the current day.
 * Called on every ON_RESUME from the dashboard screen.
 */
class DashboardViewModel(
    private val dataSourceRepository: DataSourceRepository,
    private val callLogRepository: CallLogRepository,
    private val settingRepository: SettingRepository,
    private val sourceSyncUseCase: SourceSyncUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "DashboardViewModel"
    }

    val totalSources: Flow<Int> = dataSourceRepository.getSourceCount()
    val totalEntries: Flow<Int> = dataSourceRepository.getTotalEntryCount()

    private val _blockedToday = MutableStateFlow(0)
    val blockedToday: StateFlow<Int> = _blockedToday.asStateFlow()

    val dataSources: Flow<List<SourceEntity>> = dataSourceRepository.getAllSources()
    val enabledSourcesCount: Flow<Int> = dataSourceRepository.getEnabledSourceCount()
    val enabledSourcesEntryCount: Flow<Int> = dataSourceRepository.getEnabledSourcesEntryCount()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _ledStates = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val ledStates: StateFlow<Map<Int, Boolean>> = _ledStates.asStateFlow()

    // Shield active = OS has granted ROLE_CALL_SCREENING.
    // Rechecked on every ON_RESUME via checkShieldStatus(context).
    // Never cached between resumes — role can be revoked at any time.
    private val _shieldActive = MutableStateFlow(false)
    val shieldActive: StateFlow<Boolean> = _shieldActive.asStateFlow()

    // Calls screened today — wired to CallLogDao.getCallsInRange() (Step 2.5).
    // Refreshed on every ON_RESUME via refreshCounters().
    // Step 0.1 (2026-07-02): Now populated from CallLogDao queries.
    private val _callsScreenedToday = MutableStateFlow(0)
    val callsScreenedToday: StateFlow<Int> = _callsScreenedToday.asStateFlow()

    // Read-only from the dashboard's side — RiskThresholdStep (via OnboardingViewModel)
    // is the only writer of this key. Nullable and starting at null deliberately: this
    // must never be mistaken for "onboarding not complete" while the async load below
    // is still in flight, which would incorrectly re-launch onboarding for a returning
    // user on every app start during that brief window before the DB read resolves.
    private val _isOnboardingComplete = MutableStateFlow<Boolean?>(null)
    val isOnboardingComplete: StateFlow<Boolean?> = _isOnboardingComplete.asStateFlow()

    init {
        observeDataSources()
        loadOnboardingStatus()
    }

    private fun loadOnboardingStatus() {
        viewModelScope.launch {
            val value = settingRepository.getSettingValue(SettingKeys.ONBOARDING_COMPLETE)
            _isOnboardingComplete.value = value?.toBoolean() ?: false
        }
    }

    private fun observeDataSources() {
        viewModelScope.launch {
            dataSources.collect { sources ->
                val newLedStates = mutableMapOf<Int, Boolean>()
                sources.forEach { source ->
                    newLedStates[source.id] = source.isEnabled
                }
                _ledStates.value = newLedStates
            }
        }
    }

    /**
     * Refreshes today's counters. Called on every ON_RESUME.
     * Step 0.1 (2026-07-02): Now wired to CallLogDao queries per Step 2.5.
     * 
     * Fetches:
     * - Blocked calls count (decision = 'BLOCK') since midnight
     * - Total screened calls (all decisions) since midnight
     *
     * Updates _callsScreenedToday and _blockedToday state flows.
     */
    fun refreshCounters() {
        viewModelScope.launch {
            try {
                val todayMidnight = getTodayMidnightTimestamp()
                val now = System.currentTimeMillis()

                // Query blocked calls since midnight
                val blockedCount = callLogRepository.getBlockedCallsCount(todayMidnight)
                _blockedToday.value = blockedCount

                // Query total screened calls (all statuses) since midnight
                val screenedCount = callLogRepository.getCallsInRange(todayMidnight, now)
                _callsScreenedToday.value = screenedCount

                Timber.tag(TAG).d(
                    "Counters refreshed — Screened: $screenedCount, Blocked: $blockedCount"
                )
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to refresh call counters")
                // On error, set to 0 to avoid stale values
                _callsScreenedToday.value = 0
                _blockedToday.value = 0
            }
        }
    }

    /**
     * Calculates today's midnight timestamp (00:00:00 local time).
     * Used as the start boundary for CallLogDao range queries.
     */
    private fun getTodayMidnightTimestamp(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    /**
     * Must be called on every Lifecycle.Event.ON_RESUME from the dashboard screen.
     * Queries RoleManager directly — never relies on a previously cached value.
     * 
     * ROLE_CALL_SCREENING can be revoked at any time (user settings or another app
     * taking the role). Always re-check on resume.
     */
    fun checkShieldStatus(context: Context) {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        _shieldActive.value = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    fun toggleSourceEnabled(sourceId: Int, isEnabled: Boolean) {
        viewModelScope.launch {
            try {
                dataSourceRepository.toggleSourceEnabled(sourceId, isEnabled)
                Timber.tag(TAG).d("Source $sourceId toggled to $isEnabled")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to toggle source $sourceId")
            }
        }
    }

    fun syncSource(sourceId: Int) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val result = sourceSyncUseCase.syncSource(sourceId)
                if (result.success) {
                    Timber.tag(TAG).i("Source $sourceId accepted: ${result.entriesAdded} entries")
                } else {
                    Timber.tag(TAG).w("Source $sourceId sync failed: ${result.errorMessage}")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to sync source $sourceId")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun syncAllSources() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val enabledSourceIds = dataSources.first()
                    .filter { it.isEnabled }
                    .map { it.id }
                val results = sourceSyncUseCase.syncSources(enabledSourceIds)
                val accepted = results.count { it.success }
                Timber.tag(TAG).i("Enabled source sync complete: $accepted/${results.size} accepted")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to sync all sources")
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
