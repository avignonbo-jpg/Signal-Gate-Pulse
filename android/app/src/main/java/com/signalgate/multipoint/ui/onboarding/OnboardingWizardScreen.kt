package com.signalgate.multipoint.ui.onboarding

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.signalgate.multipoint.ui.theme.*

@Composable
fun OnboardingWizardScreen(
    navController: NavHostController,
    viewModel: OnboardingViewModel = viewModel()
) {
    NavHost(navController = navController, startDestination = "permissions") {
        composable("permissions") { PermissionsStep(navController, viewModel) }
        composable("contacts") { ContactsImportStep(navController) }
        composable("sources") { SourcesSelectionStep(navController) }
        composable("risk") { RiskThresholdStep(navController) }
    }
}

@Composable
fun PermissionsStep(navController: NavHostController, viewModel: OnboardingViewModel) {
    val context = LocalContext.current
    val permissionStates by viewModel.permissionStates.collectAsState()
    var showRationaleDialog by remember { mutableStateOf<PermissionItem?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        results.forEach { (permission, isGranted) ->
            viewModel.onPermissionResult(permission, isGranted)
        }
    }

    // Initial check
    LaunchedEffect(Unit) {
        val currentStates = viewModel.permissionsList.associate { 
            it.permission to (ContextCompat.checkSelfPermission(context, it.permission) == PackageManager.PERMISSION_GRANTED)
        }
        viewModel.updateAllPermissions(currentStates)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Security Clearances",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Grant these permissions to activate SignalGate's core shielding layers.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(viewModel.permissionsList) { permission ->
                val isGranted = permissionStates[permission.permission] ?: false
                
                Surface(
                    onClick = { 
                        if (!isGranted) showRationaleDialog = permission 
                    },
                    shape = MaterialTheme.shapes.medium,
                    color = SurfaceGlass,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = permission.title,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = permission.description,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        
                        Icon(
                            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isGranted) NeonCyan else NeonRed
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val ungranted = viewModel.permissionsList.filter { 
                    (permissionStates[it.permission] == false)
                }.map { it.permission }
                
                if (ungranted.isEmpty()) {
                    navController.navigate("contacts")
                } else {
                    launcher.launch(ungranted.toTypedArray())
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (viewModel.allRequiredGranted()) NeonCyan else SurfaceGlass,
                contentColor = if (viewModel.allRequiredGranted()) Color.Black else TextSecondary
            )
        ) {
            Text(if (viewModel.allRequiredGranted()) "Continue" else "Grant Permissions")
        }
    }

    // Rationale Dialog
    showRationaleDialog?.let { permission ->
        AlertDialog(
            onDismissRequest = { showRationaleDialog = null },
            title = { Text(permission.title) },
            text = { Text(permission.rationale) },
            confirmButton = {
                TextButton(onClick = {
                    launcher.launch(arrayOf(permission.permission))
                    showRationaleDialog = null
                }) {
                    Text("Grant")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationaleDialog = null }) {
                    Text("Not Now")
                }
            }
        )
    }
}

@Composable
fun ContactsImportStep(navController: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Contacts Synced", color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("sources") }) {
            Text("Continue")
        }
    }
}

@Composable
fun SourcesSelectionStep(navController: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Source Configuration", color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("risk") }) {
            Text("Continue")
        }
    }
}

@Composable
fun RiskThresholdStep(navController: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Risk Profile Set", color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { /* Finish onboarding */ }) {
            Text("Finish Setup")
        }
    }
}
