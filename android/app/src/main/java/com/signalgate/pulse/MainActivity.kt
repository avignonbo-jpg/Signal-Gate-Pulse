package com.signalgate.pulse

import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.signalgate.pulse.security.DatabaseResetEvent
import com.signalgate.pulse.ui.components.GlassmorphicDrawerContent
import com.signalgate.pulse.ui.navigation.SignalGateNavGraph
import com.signalgate.pulse.ui.theme.SignalGateTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        StartupDiagnostics.mark(StartupDiagnostics.Event.ACTIVITY_ON_CREATE_BEGIN)
        // Must be called before super.onCreate() per the SplashScreen API contract.
        // See AppReadiness (MainApplication.kt) and Theme.App.Starting (themes.xml).
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !AppReadiness.isReady.value }

        super.onCreate(savedInstanceState)

        window.decorView.viewTreeObserver.addOnDrawListener(object : ViewTreeObserver.OnDrawListener {
            private var recorded = false

            override fun onDraw() {
                if (!recorded) {
                    recorded = true
                    StartupDiagnostics.mark(StartupDiagnostics.Event.ACTIVITY_FIRST_FRAME)
                    window.decorView.post {
                        window.decorView.viewTreeObserver.removeOnDrawListener(this)
                    }
                }
            }
        })

        setContent {
            SignalGateTheme {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val wasDatabaseReset by DatabaseResetEvent.wasReset.collectAsState()
                if (wasDatabaseReset) {
                    AlertDialog(
                        onDismissRequest = { DatabaseResetEvent.acknowledge() },
                        title = { Text("Protection database was reset") },
                        text = {
                            Text(
                                "Your device's secure storage changed in a way that made the " +
                                "existing call-screening data unreadable (this can happen after " +
                                "certain lock-screen or security changes). A fresh, empty " +
                                "database has been created — you may want to re-review your " +
                                "blocklist and settings."
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { DatabaseResetEvent.acknowledge() }) {
                                Text("OK")
                            }
                        }
                    )
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        GlassmorphicDrawerContent(
                            currentRoute = currentRoute,
                            onDestinationSelected = { screen ->
                                scope.launch { drawerState.close() }
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                ) {
                    Scaffold { innerPadding ->
                        SignalGateNavGraph(
                            navController = navController,
                            modifier = Modifier.padding(innerPadding),
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }
                }
            }
        }
        StartupDiagnostics.mark(StartupDiagnostics.Event.ACTIVITY_CONTENT_SET)
    }
}
