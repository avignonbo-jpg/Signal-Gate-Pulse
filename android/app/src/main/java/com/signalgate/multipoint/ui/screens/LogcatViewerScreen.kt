package com.signalgate.multipoint.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.signalgate.multipoint.BuildConfig
import com.signalgate.multipoint.ui.viewmodels.LogcatViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * In-app Logcat viewer screen.
 * Guarded by BuildConfig.DEBUG — renders a blank screen in release builds.
 */
@Composable
fun LogcatViewerScreen(
    viewModel: LogcatViewModel = koinViewModel()
) {
    if (!BuildConfig.DEBUG) return

    val logs by viewModel.logs.collectAsState()

    LaunchedEffect(Unit) { viewModel.captureLogcat() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = { viewModel.captureLogcat() }) {
            Text("Refresh Logs")
        }
        LazyColumn {
            items(logs) { log ->
                Text(text = log, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
