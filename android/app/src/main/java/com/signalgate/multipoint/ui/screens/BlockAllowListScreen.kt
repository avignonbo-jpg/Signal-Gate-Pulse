package com.signalgate.multipoint.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.signalgate.multipoint.database.entities.UnifiedEntryEntity
import com.signalgate.multipoint.ui.BlockedNumbersViewModel
import com.signalgate.multipoint.ui.components.GlassCard
import com.signalgate.multipoint.utils.humanReadable
import org.koin.androidx.compose.koinViewModel

/**
 * BlockAllowListScreen — Phase 4.2 (Contract §4 L7).
 *
 * Was `Text("Block / Allow List — Coming Soon")` in NavGraph.kt. Built
 * against BlockedNumbersViewModel / BlocklistRepository per the
 * Production-Readiness Procedure's own acceptance criteria: search, filter
 * by BLOCK/ALLOW, manual add, and delete — all backed by the user's own
 * MANUAL-source rules, not the aggregate of every synced source. Sheet
 * visibility and form-error state live on the ViewModel, matching
 * SourcesScreen's Add Source flow.
 */
@Composable
fun BlockAllowListScreen(viewModel: BlockedNumbersViewModel = koinViewModel()) {
    val rules by viewModel.visibleRules.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val formError by viewModel.formError.collectAsState()
    val isAddSheetVisible by viewModel.isAddSheetVisible.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Block / Allow Lists") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showAddSheet() },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Number") }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp)) {
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by number") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                }
            )

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filter == BlockedNumbersViewModel.Filter.ALL,
                    onClick = { viewModel.setFilter(BlockedNumbersViewModel.Filter.ALL) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = filter == BlockedNumbersViewModel.Filter.BLOCKED,
                    onClick = { viewModel.setFilter(BlockedNumbersViewModel.Filter.BLOCKED) },
                    label = { Text("Blocked") }
                )
                FilterChip(
                    selected = filter == BlockedNumbersViewModel.Filter.ALLOWED,
                    onClick = { viewModel.setFilter(BlockedNumbersViewModel.Filter.ALLOWED) },
                    label = { Text("Allowed") }
                )
            }

            Spacer(Modifier.height(10.dp))

            if (isLoading && rules.isEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (!isLoading && rules.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (searchQuery.isBlank()) {
                            "No manual rules yet. Tap \"Add Number\" to block or allow a number."
                        } else {
                            "No numbers match \"$searchQuery\"."
                        }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(rules, key = { it.id }) { rule ->
                        RuleRow(rule = rule, onDelete = { viewModel.deleteRule(rule) })
                    }
                }
            }
        }
    }

    if (isAddSheetVisible) {
        AddRuleBottomSheet(
            error = formError,
            onDismiss = { viewModel.hideAddSheet() },
            onConfirm = { number, action, reason -> viewModel.addRule(number, action, reason) }
        )
    }
}

@Composable
private fun RuleRow(rule: UnifiedEntryEntity, onDelete: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(rule.phoneNumber, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = rule.action,
                    color = actionColor(rule.action),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                rule.metadata ?: "Manual rule",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Added ${rule.createdAt.humanReadable()}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDelete) { Text("Remove") }
            }
        }
    }
}

private fun actionColor(action: String): Color = when (action.uppercase()) {
    "BLOCK" -> Color(0xFFE74C3C)
    "ALLOW" -> Color(0xFF2ECC71)
    else -> Color(0xFF95A5A6)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRuleBottomSheet(
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (number: String, action: String, reason: String) -> Unit
) {
    var number by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("BLOCK") }
    var reason by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Add Number", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = number,
                onValueChange = { number = it },
                label = { Text("Phone number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            Text("Action", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = action == "BLOCK",
                    onClick = { action = "BLOCK" },
                    label = { Text("Block") }
                )
                FilterChip(
                    selected = action == "ALLOW",
                    onClick = { action = "ALLOW" },
                    label = { Text("Allow") }
                )
            }
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Note (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onConfirm(number, action, reason) }) { Text("Save") }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
