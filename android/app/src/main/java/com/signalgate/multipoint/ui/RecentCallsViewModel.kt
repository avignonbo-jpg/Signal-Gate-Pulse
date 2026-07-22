package com.signalgate.multipoint.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signalgate.multipoint.database.entities.CallLogEntry
import com.signalgate.multipoint.database.entities.UnifiedEntryEntity
import com.signalgate.multipoint.database.repositories.CallLogRepository
import com.signalgate.multipoint.database.repositories.DataSourceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Uses [CallLogRepository] and [DataSourceRepository] as the production data path.
 * Raw DAO access has been removed; all mutations go through the repository layer.
 */
class RecentCallsViewModel(
    private val callLogRepository: CallLogRepository,
    private val dataSourceRepository: DataSourceRepository
) : ViewModel() {

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
            dataSourceRepository.insertEntry(
                UnifiedEntryEntity(
                    phoneNumber = phoneNumber,
                    action = "BLOCK",
                    sourceId = 1 // Source ID 1 is the MANUAL entry source
                )
            )
        }
    }

    fun whitelistNumber(phoneNumber: String) {
        viewModelScope.launch {
            dataSourceRepository.insertEntry(
                UnifiedEntryEntity(
                    phoneNumber = phoneNumber,
                    action = "ALLOW",
                    sourceId = 1 // Source ID 1 is the MANUAL entry source
                )
            )
        }
    }
}
