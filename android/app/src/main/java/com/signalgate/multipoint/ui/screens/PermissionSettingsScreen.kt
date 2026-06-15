package com.signalgate.multipoint.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.signalgate.multipoint.ui.components.AdvancedGlassCard
import com.signalgate.multipoint.ui.theme.*

data class DetailedPermission(
    val name: String,
    val manifestString: String,
    val description: String,
    val rationale: String,
    val isRequired: Boolean
)

@Composable
fun PermissionSettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    val allPermissions = remember {
        mutableListOf(
            DetailedPermission(
                "Phone Connection",
                Manifest.permission.READ_PHONE_STATE,
                "Detect incoming calls for screening.",
                "SignalGate needs to know when your phone is receiving a call so it can analyze the signal before it reaches you.",
                true
            ),
            DetailedPermission(
                "Identity Verification",
                Manifest.permission.READ_PHONE_NUMBERS,
                "Verify your own number for routing.",
                "We need to verify your identity to ensure the call screening service is correctly configured for your specific line.",
                true
            ),
            DetailedPermission(
                "Call Control",
                Manifest.permission.ANSWER_PHONE_CALLS,
                "Allow the app to manage calls.",
                "This allows SignalGate to automatically handle calls based on your security settings, saving you from manual effort.",
                true
            ),
            DetailedPermission(
                "Call History",
                Manifest.permission.READ_CALL_LOG,
                "Show history of screened calls.",
                "Accessing your call log allows you to see exactly which calls were blocked or allowed in your dashboard history.",
                false
            ),
            DetailedPermission(
                "Log Management",
                Manifest.permission.WRITE_CALL_LOG,
                "Clean up spam from your history.",
                "This allows SignalGate to mark or remove identified spam calls from your history, keeping your log clean.",
                false
            ),
            DetailedPermission(
                "Contacts Access",
                Manifest.permission.READ_CONTACTS,
                "Automatically allow known contacts.",
                "By reading your contacts, SignalGate can instantly recognize your friends and family, ensuring they never get blocked.",
                false
            )
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(
                    DetailedPermission(
                        "Security Alerts",
                        Manifest.permission.POST_NOTIFICATIONS,
                        "Get notified about blocked threats.",
                        "Enable notifications so we can alert you in real-time when a suspicious call is intercepted.",
                        true
                    )
                )
            }
        }.toList()
    }

    var permissionsState by remember { 
        mutableStateOf(allPermissions.associate { it.manifestString to false }) 
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Refresh all states after any permission change
        val newState = allPermissions.associate { it.manifestString to (
            ContextCompat.checkSelfPermission(context, it.manifestString) == PackageManager.PERMISSION_GRANTED
        )}
        permissionsState = newState
    }

    // Initial audit
    LaunchedEffect(Unit) {
        val initialState = allPermissions.associate { it.manifestString to (
            ContextCompat.checkSelfPermission(context, it.manifestString) == PackageManager.PERMISSION_GRANTED
        )}
        permissionsState = initialState
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "SYSTEM ACCESS COMPLIANCE",
            color = TextPrimary,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = "Audit and toggle security hooks below. Green indicates active protection.",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(allPermissions) { permission ->
                val isGranted = permissionsState[permission.manifestString] ?: false
                
                AdvancedGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(permission.name, color = TextPrimary, fontSize = 16.sp)
                                if (permission.isRequired) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "REQUIRED",
                                        color = NeonRed,
                                        fontSize = 10.sp,
                                        style = androidx.compose.ui.text.TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    )
                                }
                            }
                            Text(permission.description, color = TextSecondary, fontSize = 12.sp)
                        }

                        Switch(
                            checked = isGranted,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    launcher.launch(permission.manifestString)
                                } else {
                                    // Guide to settings for revocation
                                    openApplicationSettings(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = SurfaceGlass,
                                uncheckedThumbColor = TextSecondary
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun openApplicationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}
