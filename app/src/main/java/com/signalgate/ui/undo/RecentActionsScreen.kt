package com.signalgate.ui.undo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun RecentActionsScreen(onUndo: (String) -> Unit) {
    // List last 20 blocks with Undo button
    Column {
        Text("Recent Actions - Tap to Undo")
        // LazyColumn with block history + undo buttons
    }
}
