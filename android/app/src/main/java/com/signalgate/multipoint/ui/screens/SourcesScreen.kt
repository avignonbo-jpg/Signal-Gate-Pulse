package com.signalgate.multipoint.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
 * Real SourceEntity data via SourcesViewModel -> DataSourceRepository, health
 * status (green/yellow/red), human-readable last-sync timestamp, and a
 * working "Add Source" bottom sheet that inserts through the repository.
 *
 * Fixed per Production-Readiness Procedure, Phase 0.1 — this file previously
 * referenced a SourcesViewModel class that did not exist anywhere, along
 * with an unimported Color and an undefined Long.humanReadable() extension,
 * and would not compile.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(viewModel: SourcesViewModel = koinViewModel()) {
    val sources by viewModel.sources.collectAsState(initial = emptyList())
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isAddSheetVisible by viewModel.isAddSheetVisible.collectAsState()
    val addSourceError by viewModel.addSourceError.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Data Sources") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showAddSheet() },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Source") }
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
                    Text("No sources yet. Tap \"Add Source\" to get started.")
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
                            onDelete = { viewModel.deleteSource(source) }
                        )
                    }
                }
            }
        }
    }

    if (isAddSheetVisible) {
        AddSourceBottomSheet(
            error = addSourceError,
            onDismiss = { viewModel.hideAddSheet() },
            onConfirm = { name, type, pathOrUrl, priority ->
                viewModel.addSource(name, type, pathOrUrl, priority)
            }
        )
    }
}

@Composable
private fun SourceRow(
    source: SourceEntity,
    onSync: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(source.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = source.healthStatus,
                    color = getHealthColor(source.healthStatus),
                    style = MaterialTheme.typography.labelMedium
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSourceBottomSheet(
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, pathOrUrl: String, priority: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(SourcesViewModel.ALLOWED_TYPES.first()) }
    var pathOrUrl by remember { mutableStateOf("") }
    var priorityText by remember { mutableStateOf("50") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text("Add Source", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            Text("Type", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SourcesViewModel.ALLOWED_TYPES.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { type = option },
                        label = { Text(option) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = pathOrUrl,
                onValueChange = { pathOrUrl = it },
                label = { Text("Path or URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = priorityText,
                onValueChange = { input -> if (input.all { it.isDigit() }) priorityText = input },
                label = { Text("Priority (0–99)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    onConfirm(name, type, pathOrUrl, priorityText.toIntOrNull() ?: 50)
                }) { Text("Save") }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
