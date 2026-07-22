package com.signalgate.multipoint.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.signalgate.multipoint.ui.digest.DigestScreen
import com.signalgate.multipoint.ui.onboarding.OnboardingWizardScreen
import com.signalgate.multipoint.ui.screens.CallLogScreen
import com.signalgate.multipoint.ui.screens.LogcatViewerScreen
import com.signalgate.multipoint.ui.screens.OperationalDashboard
import com.signalgate.multipoint.ui.screens.SettingsScreen
import com.signalgate.multipoint.ui.screens.SourcesScreen
import com.signalgate.multipoint.ui.screens.BlockAllowListScreen

@Composable
fun SignalGateNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier.fillMaxSize()
    ) {
        composable(Screen.Dashboard.route) {
            OperationalDashboard(
                onOpenDrawer = onOpenDrawer,
                onLaunchOnboarding = { navController.navigate(Screen.Onboarding.route) }
            )
        }

        composable(Screen.Sources.route) {
            SourcesScreen()
        }

        composable(Screen.CallLog.route) {
            CallLogScreen()
        }

        composable(Screen.BlockAllowList.route) {
            BlockAllowListScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToLogcat = { navController.navigate(Screen.Logcat.route) }
            )
        }

        composable(Screen.Logcat.route) {
            LogcatViewerScreen()
        }

        composable(Screen.Onboarding.route) {
            OnboardingWizardScreen(navController)
        }

        /**
         * Screen.Digest — blocked call review queue.
         *
         * Reached two ways:
         *   1. Deep link: notification tap fires signalgate://digest PendingIntent
         *      → MainActivity receives → NavController routes here automatically.
         *   2. Direct navigation: navController.navigate(Screen.Digest.route)
         *      from the consumer dashboard "View Recent Activity" link.
         *
         * The uriPattern must match android:scheme + android:host declared in
         * AndroidManifest.xml MainActivity intent-filter.
         */
        composable(
            route = Screen.Digest.route,
            deepLinks = listOf(navDeepLink { uriPattern = "signalgate://digest" })
        ) {
            DigestScreen()
        }
    }
}
