package com.signalgate.pulse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signalgate.pulse.database.entities.CallLogEntry
import com.signalgate.pulse.database.entities.UnifiedEntryEntity
import com.signalgate.pulse.database.repositories.CallLogRepository
import com.signalgate.pulse.database.repositories.DataSourceRepository
import com.signalgate.pulse.data.models.CallLogItem
import com.signalgate.pulse.data.models.CallType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Uses [CallLogRepository] and [DataSourceRepository] as the production data path.
 * Raw DAO access has been removed; all mutations go through the repository layer.
 */
class RecentCallsViewModel(
    private val callLogRepository: CallLogRepository,
    private val dataSourceRepository: DataSourceRepository
) : ViewModel() {

    // Preserves TelemetryViewModel’s UI transformation under the screen-owned
    // RecentCallsViewModel, eliminating the orphaned duplicate owner.
    private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    val liveCallTelemetry: StateFlow<List<CallLogItem>> = callLogRepository.allLogsFlow
        .map { entityList ->
            entityList.map { entity ->
                CallLogItem(
                    id = entity.id.toString(),
                    phoneNumber = entity.phoneNumber,
                    location = "Unknown Location",
                    timestamp = dateFormat.format(Date(entity.timestamp)),
                    type = when (entity.decision) {
                        "BLOCK" -> CallType.BLOCKED
                        "ALLOW" -> CallType.INCOMING
                        "SCREEN" -> CallType.SPAM
                        else -> CallType.INCOMING
                    },
                    matchedSources = entity.matchedSources?.split(",") ?: emptyList(),
                    riskConfidence = entity.confidence ?: 0
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _recentCalls = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val recentCalls: StateFlow<List<CallLogEntry>> = _recentCalls

    init {
        loadRecentCalls()
    }

    fun loadRecentCalls() {
        viewModelScope.launch {
            callLogRepository.allLogsFlow.collect { entries ->
                _recentCalls.value = entries
            }
        }
    }

    fun blockNumber(phoneNumber: String) {
        viewModelScope.launch {
            dataSourceRepository.insertEntriesAuthoritative(
                listOf(
                    UnifiedEntryEntity(
                        phoneNumber = phoneNumber,
                        action = "BLOCK",
                        sourceId = 1 // Source ID 1 is the MANUAL entry source
                    )
                )
            )
            dataSourceRepository.rebuildDerivedIndexes()
        }
    }

    fun whitelistNumber(phoneNumber: String) {
        viewModelScope.launch {
            dataSourceRepository.insertEntriesAuthoritative(
                listOf(
                    UnifiedEntryEntity(
                        phoneNumber = phoneNumber,
                        action = "ALLOW",
                        sourceId = 1 // Source ID 1 is the MANUAL entry source
                    )
                )
            )
            dataSourceRepository.rebuildDerivedIndexes()
        }
    }
}
