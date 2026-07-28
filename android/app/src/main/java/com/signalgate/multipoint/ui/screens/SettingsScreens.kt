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
 * SettingsScreen — Application configuration and customization.
 *
 * Features:
 * - Shield color customization via RGB sliders
 * - Real-time color preview
 * - Logcat viewer access
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
 *
 * Step 0.1 (2026-07-02):
 * - Marked shield colors for migration to SettingEntry (Step 2.6)
 * - Still uses SharedPreferences for now (backward compatible)
 * - Will transition to SettingEntry when Step 2.6 is implemented
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateToLogcat: () -> Unit = {}) {
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
    }

    /**
     * Shield color state — RGB values 0-255.
     * Default: Neon cyan (66, 133, 244)
     *
     * Step 0.1 (2026-07-02): Still loaded from SharedPreferences.
     * PULSE-TODO (2026-06): Migrate to SettingEntry — Step 2.6.
     * 
     * Migration path:
     * 1. Step 2.6: Implement SettingEntry keys for shield_red, shield_green, shield_blue
     * 2. Step 2.6: Read colors from SettingDao instead of SharedPreferences
     * 3. Step 2.6: Write colors to SettingDao instead of SharedPreferences
     * 4. Step 2.6: Remove SharedPreferences fallback
     */
    var red by remember { 
        mutableFloatStateOf(sharedPreferences.getInt("shield_red", 66).toFloat()) 
    }
    var green by remember { 
        mutableFloatStateOf(sharedPreferences.getInt("shield_green", 133).toFloat()) 
    }
    var blue by remember { 
        mutableFloatStateOf(sharedPreferences.getInt("shield_blue", 244).toFloat()) 
    }

    var showDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SignalGate Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
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
            // ── Section: Shield Color ────────────────────────────────────────────
            Text(
                "Shield Color Preview",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Customize the color of the call screening shield overlay",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Real-time color preview box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        color = Color(red.toInt(), green.toInt(), blue.toInt()),
                        shape = MaterialTheme.shapes.medium
                    )
            )

            // RGB Sliders
            ColorSlider(
                label = "Red (${red.toInt()})",
                value = red,
                onValueChange = { red = it },
                color = Color.Red
            )
            ColorSlider(
                label = "Green (${green.toInt()})",
                value = green,
                onValueChange = { green = it },
                color = Color.Green
            )
            ColorSlider(
                label = "Blue (${blue.toInt()})",
                value = blue,
                onValueChange = { blue = it },
                color = Color.Blue
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Action: Save Colors ────────────────────────────────────────────
            /**
             * Saves the current RGB values to SharedPreferences.
             * Step 0.1 (2026-07-02): Still uses SharedPreferences.
             * Will migrate to SettingEntry in Step 2.6.
             */
            Button(
                onClick = {
                    // Write to SharedPreferences
                    sharedPreferences.edit().apply {
                        putInt("shield_red", red.toInt())
                        putInt("shield_green", green.toInt())
                        putInt("shield_blue", blue.toInt())
                        apply()
                    }

                    // Show confirmation dialog
                    dialogTitle = "Colors Saved"
                    dialogMessage = "Your shield color has been updated. " +
                            "Some UI elements may require an app restart to fully reflect the change."
                    showDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Shield Color")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Section: Tools ──────────────────────────────────────────────────
            Text(
                "Developer Tools",
                style = MaterialTheme.typography.titleMedium
            )

            /**
             * Opens in-app logcat viewer for debugging.
             * Useful for developers inspecting call screening logs in real-time.
             */
            OutlinedButton(
                onClick = onNavigateToLogcat,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open In-App Logcat Viewer")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Section: About ──────────────────────────────────────────────────
            Text(
                "About",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "SignalGate Pulse v1.0.0",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Real-time call screening with on-device processing. No data shared with external servers.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    /**
     * Generic confirmation dialog for settings changes.
     * Used for color save confirmation and other actions.
     */
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(dialogTitle) },
            text = { Text(dialogMessage) },
            confirmButton = {
                Button(
                    onClick = { showDialog = false }
                ) {
                    Text("OK")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

/**
 * ColorSlider — Reusable RGB component slider.
 *
 * @param label Display label with current value (e.g., "Red (128)")
 * @param value Current slider value (0-255)
 * @param onValueChange Callback when slider moves
 * @param color Visual color for the slider thumb and track
 *
 * Features:
 * - Smooth real-time feedback
 * - 0-255 range for standard RGB
 * - Color-coded thumb and active track
 */
@Composable
fun ColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    color: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color.copy(alpha = 0.7f),
                inactiveTrackColor = color.copy(alpha = 0.2f)
            ),
            steps = 254  // Discrete steps for each integer value
        )
    }
}
