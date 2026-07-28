package com.signalgate.multipoint.ui.screens

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.signalgate.multipoint.ui.components.AdvancedGlassCard
import com.signalgate.multipoint.ui.onboarding.OnboardingViewModel
import com.signalgate.multipoint.ui.theme.*
import org.koin.androidx.compose.koinViewModel

/**
 * Permission Health Check screen — Step 1.10.
 *
 * Surfaces all three Android grant mechanisms in one place, re-evaluating live
 * state every time the screen resumes (never cached):
 *   1. Standard runtime permissions — sourced from OnboardingViewModel.permissions,
 *      the same canonical list the onboarding wizard uses. This screen does NOT
 *      maintain its own copy. A prior version redefined its own DetailedPermission
 *      list here, which had silently drifted from the wizard's list — exact same
 *      failure mode as the historical AppModule.kt drift incident. Single source
 *      of truth now: OnboardingViewModel.permissions.
 *   2. ROLE_CALL_SCREENING — without this role, CallScreeningService is never
 *      invoked by the OS and the entire engine is silently inert. This is the
 *      single most important check on this screen.
 *   3. Battery optimization exemption — OEM background-kill policies (Samsung,
 *      Xiaomi, Huawei, OnePlus) can prevent CallScreeningService from running
 *      reliably even when everything else is granted.
 */
@Composable
fun PermissionSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionStates by viewModel.permissionStates.collectAsState()
    val roleHeld by viewModel.callScreeningRoleHeld.collectAsState()
    var batteryExempt by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val newState = viewModel.permissions.associate { it.permission to (
            ContextCompat.checkSelfPermission(context, it.permission) == PackageManager.PERMISSION_GRANTED
        )}
        viewModel.updateAllPermissions(newState)
    }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Result comes back via the resume re-check below, not here
    }

    // Re-check everything on every resume — never rely on cached state.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val currentStates = viewModel.permissions.associate { item ->
                    item.permission to (ContextCompat.checkSelfPermission(
                        context, item.permission
                    ) == PackageManager.PERMISSION_GRANTED)
                }
                viewModel.updateAllPermissions(currentStates)
                viewModel.checkCallScreeningRole(context)
                batteryExempt = isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Initial audit on first composition
    LaunchedEffect(Unit) {
        val initialState = viewModel.permissions.associate { it.permission to (
            ContextCompat.checkSelfPermission(context, it.permission) == PackageManager.PERMISSION_GRANTED
        )}
        viewModel.updateAllPermissions(initialState)
        viewModel.checkCallScreeningRole(context)
        batteryExempt = isIgnoringBatteryOptimizations(context)
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "SYSTEM ACCESS COMPLIANCE",
            color = TextPrimary,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = "Audit and manage every system-level grant SignalGate relies on. Green indicates active protection.",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    text = "RUNTIME PERMISSIONS",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(viewModel.permissions) { permission ->
                val isGranted = permissionStates[permission.permission] ?: false
                PermissionRow(
                    title = permission.title,
                    description = permission.description,
                    isRequired = permission.isRequired,
                    isGranted = isGranted,
                    onToggleOn = { permissionLauncher.launch(permission.permission) },
                    onToggleOff = { openApplicationSettings(context) }
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Text(
                    text = "CALL SCREENING ROLE",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            item {
                PermissionRow(
                    title = "Call Screening Role",
                    description = "Without this, the shield is completely inactive — no calls will ever be screened.",
                    isRequired = true,
                    isGranted = roleHeld,
                    onToggleOn = {
                        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                        roleLauncher.launch(intent)
                    },
                    onToggleOff = { openApplicationSettings(context) }
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Text(
                    text = "BATTERY OPTIMIZATION",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            item {
                PermissionRow(
                    title = "Background Reliability",
                    description = "Some manufacturers (Samsung, Xiaomi, Huawei, OnePlus) kill background apps aggressively. Exempting SignalGate keeps screening reliable.",
                    isRequired = false,
                    isGranted = batteryExempt,
                    onToggleOn = {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    },
                    onToggleOff = { openApplicationSettings(context) }
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    isRequired: Boolean,
    isGranted: Boolean,
    onToggleOn: () -> Unit,
    onToggleOff: () -> Unit
) {
    AdvancedGlassCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = TextPrimary, fontSize = 16.sp)
                    if (isRequired) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "REQUIRED",
                            color = NeonRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(description, color = TextSecondary, fontSize = 12.sp)
            }

            Switch(
                checked = isGranted,
                onCheckedChange = { checked ->
                    if (checked) onToggleOn() else onToggleOff()
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

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun openApplicationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}
