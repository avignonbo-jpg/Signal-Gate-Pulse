package com.signalgate.multipoint.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.signalgate.multipoint.PostCallNotifier
import androidx.compose.material3.ExperimentalMaterial3Api
import com.signalgate.multipoint.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateToLogcat: () -> Unit = {}) {
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
    }

    // Load state dynamically from SharedPreferences
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
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                        Toast.makeText(context, "Please enable 'Display over other apps'", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Overlay permission already granted", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Check & Request Permissions")
            }

            OutlinedButton(
                onClick = onNavigateToLogcat,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open In-App Logcat Viewer")
            }

            Button(
                onClick = {
                    PostCallNotifier.show(context, "555-0123")
                    Toast.makeText(context, "Test notification sent", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test Shield Popup")
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
