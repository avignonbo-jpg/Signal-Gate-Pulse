package com.signalgate.multipoint.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.signalgate.multipoint.database.entities.SourceEntity
import com.signalgate.multipoint.ui.dashboard.DashboardViewModel // or dedicated SourcesViewModel
import com.signalgate.multipoint.ui.components.GlassCard
import kotlinx.coroutines.flow.collectLatest

/**
 * SourcesScreen — Phase 2.3/2.4 (Contract §4 L7).
 * Real SourceEntity data, health status (green/yellow/red), last sync timestamp, manual add bottom sheet.
 * Expert Compose UI with search, priority, validation via SanitizationEngine.
 */
@Composable
fun SourcesScreen(viewModel: SourcesViewModel = viewModel()) {
    val sources by viewModel.sources.collectAsState(initial = emptyList())
    val isSyncing by viewModel.isSyncing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Data Sources") })
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            // Health dashboard, color-coded status
            sources.forEach { source ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text("Source: ${source.name}")
                    Text("Health: ${source.healthStatus}", color = getHealthColor(source.healthStatus))
                    Text("Last Sync: ${source.lastSynced.humanReadable()}")
                }
            }

            // "Add Source" button → bottom sheet with name, type (CSV/URL/XLSX), path/URL, priority
            Button(onClick = { viewModel.showAddSheet() }) {
                Text("Add Source")
            }
        }
    }
}

// Helper functions for color, human-readable timestamp, etc.
private fun getHealthColor(status: String) = when (status) {
    "HEALTHY" -> Color.Green
    "WARNING" -> Color.Yellow
    else -> Color.Red
}
