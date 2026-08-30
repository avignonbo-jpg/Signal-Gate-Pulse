package com.signalgate.pulse.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard     : Screen("dashboard",  "Dashboard",         Icons.Default.Home)
    object Sources       : Screen("sources",    "Sources",           Icons.Default.List)
    object CallLog       : Screen("call_log",   "Call Log",          Icons.Default.Phone)
    object BlockAllowList: Screen("block_list", "Block / Allow Lists",Icons.Default.Lock)
    object Settings      : Screen("settings",   "Settings",          Icons.Default.Settings)
    object Logcat        : Screen("logcat",     "Logcat Viewer",     Icons.Default.Info)
    object Onboarding    : Screen("onboarding", "Onboarding",        Icons.Default.PlayArrow)
    object PermissionSettings : Screen("permission_settings", "Permission Settings", Icons.Default.Settings)

    /**
     * Blocked call digest — swipeable card list backed by PendingCardEntity queue.
     * Reached via:
     *   - Notification content tap (signalgate://digest deep link)
     *   - Navigation drawer ("Blocked Calls")
     *
     * 2026-08-15: previously also reachable via a "View Recent Activity" link on
     * the consumer dashboard — removed, since it sat directly in the scroll path
     * on dashboard open, in the way of just viewing the dashboard. This drawer
     * entry is now the only in-app (non-deep-link) way to reach this screen —
     * don't remove it from GlassmorphicDrawerContent.kt without adding another
     * real access path first. See PROJECT_LEDGER.md, 2026-08-15 entry.
     */
    object Digest        : Screen("digest",     "Blocked Calls",     Icons.Default.Notifications)
}
