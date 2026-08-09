package com.signalgate.multipoint.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.signalgate.multipoint.database.entities.SourceEntity
import com.signalgate.multipoint.ui.components.GlassCard
import com.signalgate.multipoint.utils.humanReadable
import org.koin.androidx.compose.koinViewModel

/**
 * SourcesScreen — Phase 3.3/3.4 (Contract §4 L7).
 * Real SourceEntity data via SourcesViewModel -> DataSourceRepository: health
 * status (green/yellow/red), human-readable last-sync timestamp, manual
 * sync-now, enable/disable, and removal.
 *
 * The "Add Source" custom CSV/URL/XLSX flow has been removed — it was a
 * Multi-Port (prosumer) capability, not part of the Pulse consumer design
 * (FCC + community blocklist + manual only). See SourcesViewModel for the
 * full rationale.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(viewModel: SourcesViewModel = koinViewModel()) {
    val sources by viewModel.sources.collectAsState(initial = emptyList())
    val isSyncing by viewModel.isSyncing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Sources") },
                actions = {
                    IconButton(
                        onClick = { viewModel.syncAllSources() },
                        enabled = !isSyncing
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Sync all sources")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (isSyncing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (sources.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No sources yet.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sources, key = { it.id }) { source ->
                        SourceRow(
                            source = source,
                            onSync = { viewModel.syncSource(source.id) },
                            onDelete = { viewModel.deleteSource(source) },
                            onToggleEnabled = { enabled -> viewModel.toggleSourceEnabled(source.id, enabled) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceRow(
    source: SourceEntity,
    onSync: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(source.name, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = source.healthStatus,
                        color = getHealthColor(source.healthStatus),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = source.isEnabled,
                        onCheckedChange = onToggleEnabled
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Type: ${source.type} • Priority: ${source.priority}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Last sync: ${source.lastSynced.humanReadable()} • ${source.entriesCount} entries",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSync) { Text("Sync now") }
                TextButton(onClick = onDelete) { Text("Remove") }
            }
        }
    }
}

// Health dashboard color coding — HEALTHY/WARNING/UNKNOWN/anything else (incl. ERROR).
private fun getHealthColor(status: String): Color = when (status.uppercase()) {
    "HEALTHY" -> Color(0xFF2ECC71)
    "WARNING" -> Color(0xFFF1C40F)
    "UNKNOWN" -> Color(0xFF95A5A6)
    else -> Color(0xFFE74C3C)
}
