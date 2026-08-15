package com.signalgate.pulse.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.signalgate.pulse.ui.digest.DigestScreen
import com.signalgate.pulse.ui.onboarding.OnboardingWizardScreen
import com.signalgate.pulse.ui.screens.CallLogScreen
import com.signalgate.pulse.ui.screens.ConsumerDashboardScreen
import com.signalgate.pulse.ui.screens.LogcatViewerScreen
import com.signalgate.pulse.ui.screens.SettingsScreen
import com.signalgate.pulse.ui.screens.SourcesScreen
import com.signalgate.pulse.ui.screens.BlockAllowListScreen

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
            ConsumerDashboardScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToActivity = { navController.navigate(Screen.Digest.route) },
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
