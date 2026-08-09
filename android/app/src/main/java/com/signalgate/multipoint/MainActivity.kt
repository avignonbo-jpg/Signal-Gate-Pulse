package com.signalgate.multipoint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.signalgate.multipoint.security.DatabaseResetEvent
import com.signalgate.multipoint.ui.components.GlassmorphicDrawerContent
import com.signalgate.multipoint.ui.navigation.SignalGateNavGraph
import com.signalgate.multipoint.ui.theme.SignalGateTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
    }
}
