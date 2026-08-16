package com.signalgate.pulse.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.signalgate.pulse.BuildConfig
import com.signalgate.pulse.ui.viewmodels.LogcatViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * In-app Logcat viewer screen.
 * Guarded by BuildConfig.DEBUG — renders a blank screen in release builds.
 *
 * 2026-08-15: added "Copy to Clipboard" button, so logs can be pulled off a
 * real device with no adb/desktop access — user pastes clipboard contents
 * directly into chat/email. Copies exactly what's currently displayed
 * (the last captureLogcat() snapshot), not a live re-capture — tap Refresh
 * first if you want the very latest lines before copying.
 */
@Composable
fun LogcatViewerScreen(
    viewModel: LogcatViewModel = koinViewModel()
) {
    if (!BuildConfig.DEBUG) return

    val logs by viewModel.logs.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.captureLogcat() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row {
            Button(onClick = { viewModel.captureLogcat() }) {
                Text("Refresh Logs")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(onClick = { copyLogsToClipboard(context, logs) }) {
                Text("Copy to Clipboard")
            }
        }
        LazyColumn {
            items(logs) { log ->
                Text(text = log, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun copyLogsToClipboard(context: Context, logs: List<String>) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("SignalGate Pulse Logcat", logs.joinToString("\n"))
    clipboard.setPrimaryClip(clip)
}
