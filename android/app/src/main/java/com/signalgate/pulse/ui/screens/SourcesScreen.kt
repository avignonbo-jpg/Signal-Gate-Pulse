package com.signalgate.pulse.ui.screens

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
import com.signalgate.pulse.database.entities.SourceEntity
import com.signalgate.pulse.database.repositories.DataSourceRepository
import com.signalgate.pulse.logic.ReliableSourceManager
import com.signalgate.pulse.ui.components.GlassCard
import com.signalgate.pulse.utils.humanReadable
import org.koin.androidx.compose.koinViewModel

/**
 * SourcesScreen — Phase 3.4 (Contract §4 L7).
 * Real SourceEntity data via SourcesViewModel -> DataSourceRepository: explicit
 * lifecycle state, accepted-snapshot metadata, manual sync-now, enable/disable,
 * and removal. No phone-number payload is rendered on this surface.
 *
 * The source model is fixed: FTC Do Not Call, FCC Consumer Complaints,
 * Manual User Rules, and Contacts Allow List. Only FTC and FCC have remote
 * sync status; Manual User Rules shows read-only enabled status and a management
 * hint, while Contacts Allow List retains its enable/disable control.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(viewModel: SourcesViewModel = koinViewModel()) {
    val sources by viewModel.sources.collectAsState(initial = emptyList())
    val isSyncing by viewModel.isSyncing.collectAsState()
    val sourceActionError by viewModel.sourceActionError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(sourceActionError) {
        sourceActionError?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSourceActionError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
    val isRemoteSource = ReliableSourceManager.isManagedFederalSource(source)
    val isProtectedSource = source.type in DataSourceRepository.PROTECTED_SOURCE_TYPES
    val isManualUserRules = source.type == "MANUAL" && source.pathOrUrl == "local"

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(source.displaySourceName(), style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isRemoteSource) {
                        val isNotSyncedYet =
                            source.lastAcceptedSnapshot == null && source.lastAttemptedSync == null
                        Text(
                            text = if (isNotSyncedYet) "Not synced yet" else source.lifecycleState,
                            color = if (isNotSyncedYet) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                getLifecycleColor(source.lifecycleState)
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    if (isManualUserRules) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (source.isEnabled) "Enabled" else "Disabled",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (source.isEnabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(
                                text = "Manage elsewhere",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Switch(
                            checked = source.isEnabled,
                            onCheckedChange = onToggleEnabled
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Type: ${source.type} • Priority: ${source.priority}",
                style = MaterialTheme.typography.bodySmall
            )
            if (isRemoteSource) {
                Text(
                    "Last accepted: ${source.lastAcceptedSnapshot?.humanReadable() ?: "Never"} • " +
                        "${source.acceptedRecordCount ?: source.entriesCount} entries",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Last attempted: ${source.lastAttemptedSync?.humanReadable() ?: "Never"}",
                    style = MaterialTheme.typography.bodySmall
                )
                source.lifecycleReason?.takeIf { it.isNotBlank() }?.let { reason ->
                    Text(
                        "Status reason: ${reason.take(120)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isRemoteSource) TextButton(onClick = onSync) { Text("Sync now") }
                if (!isProtectedSource) TextButton(onClick = onDelete) { Text("Remove") }
            }
        }
    }
}

private fun SourceEntity.displaySourceName(): String = when (type) {
    "FTC" -> "FTC Do Not Call Registry"
    "FCC" -> "FCC Consumer Complaints"
    else -> name
}

// Lifecycle color coding. State text is persisted and never derived from entry count.
private fun getLifecycleColor(state: String): Color = when (state.uppercase()) {
    "HEALTHY" -> Color(0xFF2ECC71)
    "ENABLED", "SYNCING" -> Color(0xFF3498DB)
    "STALE" -> Color(0xFFF1C40F)
    "DISABLED" -> Color(0xFF95A5A6)
    "FAILED", "REJECTED" -> Color(0xFFE74C3C)
    else -> Color(0xFF95A5A6)
}
