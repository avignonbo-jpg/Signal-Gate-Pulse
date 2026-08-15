package com.signalgate.pulse.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

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
 * Step 2.6 (complete): shield colors now owned by SettingsViewModel, backed by
 * SettingEntry via SettingRepository. This Composable no longer touches
 * SharedPreferences at all — it only reads SettingsViewModel's state flows and
 * calls onSliderChange/saveShieldColor. See SettingsViewModel's doc comment
 * for how this also resolves half of Phase 0's FLAG-1.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToLogcat: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel()
) {
    val red by viewModel.shieldRed.collectAsState()
    val green by viewModel.shieldGreen.collectAsState()
    val blue by viewModel.shieldBlue.collectAsState()
    val saveConfirmed by viewModel.saveConfirmed.collectAsState()

    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(saveConfirmed) {
        if (saveConfirmed) {
            showDialog = true
        }
    }

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
                        color = Color(red, green, blue),
                        shape = MaterialTheme.shapes.medium
                    )
            )

            // RGB Sliders
            ColorSlider(
                label = "Red ($red)",
                value = red.toFloat(),
                onValueChange = { viewModel.onSliderChange(it.toInt(), green, blue) },
                color = Color.Red
            )
            ColorSlider(
                label = "Green ($green)",
                value = green.toFloat(),
                onValueChange = { viewModel.onSliderChange(red, it.toInt(), blue) },
                color = Color.Green
            )
            ColorSlider(
                label = "Blue ($blue)",
                value = blue.toFloat(),
                onValueChange = { viewModel.onSliderChange(red, green, it.toInt()) },
                color = Color.Blue
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Action: Save Colors ────────────────────────────────────────────
            Button(
                onClick = { viewModel.saveShieldColor() },
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
     * Shield-color save confirmation. Shown reactively via saveConfirmed rather
     * than set imperatively at the button's onClick — the dialog only appears
     * once SettingsViewModel confirms the SettingRepository writes actually
     * completed, not merely that the button was tapped.
     */
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                viewModel.acknowledgeSave()
            },
            title = { Text("Colors Saved") },
            text = {
                Text(
                    "Your shield color has been updated. Some UI elements may " +
                        "require an app restart to fully reflect the change."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        viewModel.acknowledgeSave()
                    }
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
