package com.signalgate.multipoint.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signalgate.multipoint.data.models.CallLogItem
import com.signalgate.multipoint.data.models.CallType
import com.signalgate.multipoint.database.repositories.CallLogRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class TelemetryViewModel(
    private val repository: CallLogRepository
) : ViewModel() {

    // Transforms database entities into UI-consumable data models on the fly
    private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    val liveCallTelemetry: StateFlow<List<CallLogItem>> = repository.allLogsFlow
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
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Drop connection when UI backgrounded
            initialValue = emptyList()
        )
}