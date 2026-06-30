package com.signalgate.multipoint.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * SettingsScreen.
 *
 * Dead-path cleanup (2026-06):
 * - "Check & Request Permissions" button removed. It called
 *   Settings.ACTION_MANAGE_OVERLAY_PERMISSION — SYSTEM_ALERT_WINDOW is banned
 *   from Pulse entirely (Architecture Contract v5). Permission management now
 *   lives exclusively in the Permission Health Check screen (Step 1.10), which
 *   covers runtime permissions, ROLE_CALL_SCREENING, and battery optimization
 *   without any overlay-permission path.
 * - "Test Shield Popup" button removed along with its PostCallNotifier.show()
 *   call. PostCallNotifier.kt has been deleted — it was a dead, untiered
 *   notification path that conflicted with SignalGateCallScreeningService's
 *   five-tier system. See PhoneStateReceiver.kt class doc for full context.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateToLogcat: () -> Unit = {}) {
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
    }

    // Load state dynamically from SharedPreferences.
    // PULSE-TODO (2026-06): migrate shield_red/green/blue to SettingEntry — Step 2.6.
    var red by remember { mutableStateOf(sharedPreferences.getInt("shield_red", 66).toFloat()) }
    var green by remember { mutableStateOf(sharedPreferences.getInt("shield_green", 133).toFloat()) }
    var blue by remember { mutableStateOf(sharedPreferences.getInt("shield_blue", 244).toFloat()) }

    var showDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("SignalGate Settings") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Color Preview Shield
            Text("Shield Color Preview", style = MaterialTheme.typography.titleMedium)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color(red.toInt(), green.toInt(), blue.toInt()))
            )

            // RGB Sliders
            ColorSlider(label = "Red (${red.toInt()})", value = red, onValueChange = { red = it }, color = Color.Red)
            ColorSlider(label = "Green (${green.toInt()})", value = green, onValueChange = { green = it }, color = Color.Green)
            ColorSlider(label = "Blue (${blue.toInt()})", value = blue, onValueChange = { blue = it }, color = Color.Blue)

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Button(
                onClick = {
                    sharedPreferences.edit().apply {
                        putInt("shield_red", red.toInt())
                        putInt("shield_green", green.toInt())
                        putInt("shield_blue", blue.toInt())
                        apply()
                    }
                    Toast.makeText(context, "Theme color saved", Toast.LENGTH_SHORT).show()
                    dialogTitle = "Restart Recommended"
                    dialogMessage = "Your new theme color has been saved.\n\nSome UI elements may require an app restart to fully update."
                    showDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply Theme Color")
            }

            OutlinedButton(
                onClick = onNavigateToLogcat,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open In-App Logcat Viewer")
            }
        }
    }

    // Generic AlertDialog for Info/Alerts
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(dialogTitle) },
            text = { Text(dialogMessage) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ColorSlider(label: String, value: Float, onValueChange: (Float) -> Unit, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color.copy(alpha = 0.5f)
            )
        )
    }
}
