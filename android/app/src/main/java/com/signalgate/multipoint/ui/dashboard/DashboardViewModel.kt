package com.signalgate.multipoint.ui.dashboard

import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signalgate.multipoint.database.entities.SourceEntity
import com.signalgate.multipoint.database.repositories.DataSourceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: DataSourceRepository
) : ViewModel() {

    val totalSources: Flow<Int> = repository.getSourceCount()
    val totalEntries: Flow<Int> = repository.getTotalEntryCount()

    private val _blockedToday = MutableStateFlow(0)
    val blockedToday: StateFlow<Int> = _blockedToday.asStateFlow()

    val dataSources: Flow<List<SourceEntity>> = repository.getAllSources()
    val enabledSourcesCount: Flow<Int> = repository.getEnabledSourceCount()
    val enabledSourcesEntryCount: Flow<Int> = repository.getEnabledSourcesEntryCount()

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
    private val _callsScreenedToday = MutableStateFlow(0)
    val callsScreenedToday: StateFlow<Int> = _callsScreenedToday.asStateFlow()

    init {
        observeDataSources()
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
     * Wires to CallLogDao queries once Step 2.4/2.5 are implemented.
     * PULSE-TODO (2026-06): replace stub with real CallLogDao queries.
     */
    fun refreshCounters() {
        viewModelScope.launch {
            // Step 2.4/2.5 wiring point — CallLogDao queries go here
            // _blockedToday.value = callLogDao.getBlockedCallsCount(todayMidnight)
            // _callsScreenedToday.value = callLogDao.getCallsInRange(todayMidnight, Long.MAX_VALUE)
        }
    }

    /**
     * Must be called on every Lifecycle.Event.ON_RESUME from the dashboard screen.
     * Queries RoleManager directly — never relies on a previously cached value.
     */
    fun checkShieldStatus(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            _shieldActive.value = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        } else {
            _shieldActive.value = false
        }
    }

    fun toggleSourceEnabled(sourceId: Int, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleSourceEnabled(sourceId, isEnabled)
        }
    }

    fun syncSource(sourceId: Int) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val entriesCount = repository.getEntryCountBySourceId(sourceId)
                repository.updateSourceSyncStatus(
                    sourceId = sourceId,
                    timestamp = System.currentTimeMillis(),
                    entriesCount = entriesCount,
                    healthStatus = "HEALTHY"
                )
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun syncAllSources() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                dataSources.first().forEach { source ->
                    if (source.isEnabled) {
                        val entriesCount = repository.getEntryCountBySourceId(source.id)
                        repository.updateSourceSyncStatus(
                            sourceId = source.id,
                            timestamp = System.currentTimeMillis(),
                            entriesCount = entriesCount,
                            healthStatus = "HEALTHY"
                        )
                    }
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
