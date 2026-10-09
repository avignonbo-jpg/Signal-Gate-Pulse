package com.signalgate.pulse.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.signalgate.pulse.BuildConfig
import com.signalgate.pulse.diagnostics.AppLogEntry
import com.signalgate.pulse.ui.viewmodels.LogcatViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Debug-only in-app Logcat viewer. The [BuildConfig.DEBUG] guard keeps this screen
 * blank in release builds.
 *
 * The Copy both action exists so logs can be pulled off a real device with no adb
 * or desktop access. It copies exactly the currently displayed (filtered) entries
 * from both feeds; refresh the system Logcat snapshot first to include its latest lines.
 */
@Composable
fun LogcatViewerScreen(
    viewModel: LogcatViewModel = koinViewModel()
) {
    if (!BuildConfig.DEBUG) return

    val appLogs by viewModel.appLogs.collectAsState()
    val systemLogs by viewModel.systemLogs.collectAsState()
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }

    val appLines = remember(appLogs) { appLogs.map(AppLogEntry::displayLine) }
    val visibleAppLogs = remember(appLines, query) {
        appLines.filter { it.contains(query, ignoreCase = true) }
    }
    val visibleSystemLogs = remember(systemLogs, query) {
        systemLogs.filter { it.contains(query, ignoreCase = true) }
    }

    // The in-app buffer is already live; this initial capture starts the separate
    // Android Logcat snapshot without blocking or suspending that feed.
    LaunchedEffect(viewModel) { viewModel.captureLogcat() }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = { viewModel.captureLogcat() }
            ) {
                Text("Refresh system Logcat")
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = { copyLogsToClipboard(context, visibleAppLogs, visibleSystemLogs) }
            ) {
                Text("Copy both")
            }
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { viewModel.clearAppLogs() }
        ) {
            Text("Clear app buffer")
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Filter both feeds") },
            singleLine = true
        )

        Text(
            text = "App buffer is live. Android Logcat is a separate SignalGate:* snapshot; some app events can appear in both.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LogPanel(
            title = "In-app buffer · live · ${appLogs.size} entries",
            emptyMessage = "No buffered app events yet.",
            logs = visibleAppLogs,
            modifier = Modifier.weight(1f)
        )
        LogPanel(
            title = "Android Logcat · snapshot · ${systemLogs.size} lines",
            emptyMessage = "No system Logcat snapshot yet. Tap Refresh system Logcat.",
            logs = visibleSystemLogs,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LogPanel(
    title: String,
    emptyMessage: String,
    logs: List<String>,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emptyMessage, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    itemsIndexed(logs) { _, line ->
                        Text(
                            text = line,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

private fun copyLogsToClipboard(
    context: Context,
    appLogs: List<String>,
    systemLogs: List<String>
) {
    val content = buildString {
        appendLine("=== In-app buffer (visible entries) ===")
        appLogs.forEach(::appendLine)
        appendLine()
        appendLine("=== Android Logcat snapshot (SignalGate:*; visible lines) ===")
        systemLogs.forEach(::appendLine)
    }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("SignalGate Pulse logs (both sources)", content)
    clipboard.setPrimaryClip(clip)
}
