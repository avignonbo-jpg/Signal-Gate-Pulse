package com.signalgate.multipoint.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Sources : Screen("sources", "Sources", Icons.Default.List)
    object CallLog : Screen("call_log", "Call Log", Icons.Default.Phone)
    object BlockAllowList : Screen("block_list", "Block / Allow Lists", Icons.Default.Lock)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Logcat : Screen("logcat", "Logcat Viewer", Icons.Default.Info)
    object Onboarding : Screen("onboarding", "Onboarding", Icons.Default.PlayArrow)
}