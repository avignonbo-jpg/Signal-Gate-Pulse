package com.signalgate.pulse

import android.content.Context
import androidx.navigation.createGraph
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrawerNavigationBackStackTest {

    @Test
    fun drawerNavigationAfterStartupUsesDashboardNotRemovedStartupRoute() {
        val navController = createController()
        navController.navigate(DASHBOARD_ROUTE) {
            popUpTo(STARTUP_ROUTE) { inclusive = true }
        }

        assertEquals(DASHBOARD_ROUTE, navController.currentDestination?.route)
        assertThrows(IllegalArgumentException::class.java) {
            navController.getBackStackEntry(STARTUP_ROUTE)
        }

        navController.navigateToDrawerDestination(SOURCES_ROUTE)

        assertEquals(SOURCES_ROUTE, navController.currentDestination?.route)
        assertEquals(DASHBOARD_ROUTE, navController.previousBackStackEntry?.destination?.route)
    }

    @Test
    fun drawerNavigationFromDeepLinkDestinationUsesCurrentDestinationAsAnchor() {
        val navController = createController().apply {
            navigate(DIGEST_ROUTE) {
                popUpTo(STARTUP_ROUTE) { inclusive = true }
            }
        }

        navController.navigateToDrawerDestination(SOURCES_ROUTE)

        assertEquals(SOURCES_ROUTE, navController.currentDestination?.route)
        assertEquals(DIGEST_ROUTE, navController.previousBackStackEntry?.destination?.route)
    }

    @Test
    fun drawerNavigationCannotBypassStartupOrOnboarding() {
        val startupController = createController()
        startupController.navigateToDrawerDestination(DASHBOARD_ROUTE)
        assertEquals(STARTUP_ROUTE, startupController.currentDestination?.route)
        assertFalse(startupController.popBackStack())

        val onboardingController = createController().apply { navigate(ONBOARDING_ROUTE) }
        onboardingController.navigateToDrawerDestination(DASHBOARD_ROUTE)
        assertEquals(ONBOARDING_ROUTE, onboardingController.currentDestination?.route)
        assertEquals(STARTUP_ROUTE, onboardingController.previousBackStackEntry?.destination?.route)
    }

    private fun createController(): TestNavHostController {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return TestNavHostController(context).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            graph = createGraph(startDestination = STARTUP_ROUTE) {
                composable(STARTUP_ROUTE) {}
                composable(ONBOARDING_ROUTE) {}
                composable(DASHBOARD_ROUTE) {}
                composable(SOURCES_ROUTE) {}
                composable(DIGEST_ROUTE) {}
            }
        }
    }

    private companion object {
        const val STARTUP_ROUTE = "startup"
        const val ONBOARDING_ROUTE = "onboarding"
        const val DASHBOARD_ROUTE = "dashboard"
        const val SOURCES_ROUTE = "sources"
        const val DIGEST_ROUTE = "digest"
    }
}
