package com.signalgate.multipoint.ui.onboarding

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.signalgate.multipoint.ui.theme.*
import com.signalgate.multipoint.ui.viewmodels.ContactItem
import com.signalgate.multipoint.ui.viewmodels.ContactsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingWizardScreen(
    navController: NavHostController,
    viewModel: OnboardingViewModel = koinViewModel()
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionStates by viewModel.permissionStates.collectAsState()
    val roleHeld by viewModel.callScreeningRoleHeld.collectAsState()
    var showRationaleDialog by remember { mutableStateOf<PermissionItem?>(null) }

    // Role launcher — opens system dialog to grant ROLE_CALL_SCREENING
    val roleLauncher = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            // Result comes back via ON_RESUME check below, not here
        }
    } else null

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        results.forEach { (permission, isGranted) ->
            viewModel.onPermissionResult(permission, isGranted)
        }
    }

    // Re-check everything on every resume — never rely on cached state
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
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
            // Standard runtime permissions
            items(viewModel.permissions) { permission ->
                val isGranted = permissionStates[permission.permission] ?: false

                Surface(
                    onClick = { if (!isGranted) showRationaleDialog = permission },
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

            // ROLE_CALL_SCREENING — separate from runtime permissions, needs its own flow
            item {
                Surface(
                    onClick = {
                        if (!roleHeld && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                            roleLauncher?.launch(intent)
                        }
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
                                text = "Call Screening Role",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Lets SignalGate intercept and analyze every incoming call. Without this, the shield is completely inactive — no calls will ever be screened.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = if (roleHeld) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (roleHeld) NeonCyan else NeonRed
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val ungranted = viewModel.permissions
                    .filter { permissionStates[it.permission] == false }
                    .map { it.permission }

                when {
                    ungranted.isNotEmpty() -> permissionLauncher.launch(ungranted.toTypedArray())
                    !roleHeld && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                        roleLauncher?.launch(intent)
                    }
                    else -> navController.navigate("contacts")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (viewModel.allRequiredGranted() && roleHeld) NeonCyan else SurfaceGlass,
                contentColor = if (viewModel.allRequiredGranted() && roleHeld) Color.Black else TextSecondary
            )
        ) {
            Text(
                when {
                    !viewModel.allRequiredGranted() -> "Grant Permissions"
                    !roleHeld -> "Grant Call Screening Role"
                    else -> "Continue"
                }
            )
        }
    }

    showRationaleDialog?.let { permission ->
        AlertDialog(
            onDismissRequest = { showRationaleDialog = null },
            title = { Text(permission.title) },
            text = { Text(permission.rationale) },
            confirmButton = {
                TextButton(onClick = {
                    permissionLauncher.launch(arrayOf(permission.permission))
                    showRationaleDialog = null
                }) { Text("Grant") }
            },
            dismissButton = {
                TextButton(onClick = { showRationaleDialog = null }) { Text("Not Now") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsImportStep(
    navController: NavHostController,
    viewModel: ContactsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val contacts by viewModel.contacts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val filteredContacts = viewModel.filteredContacts

    LaunchedEffect(Unit) {
        viewModel.loadContacts(context)
    }

    LaunchedEffect(isSaved) {
        if (isSaved) navController.navigate("sources")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Contacts Auto-Allow",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Select contacts to automatically allow. These calls will bypass all security filters.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        TextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search contacts...", color = TextSecondary) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SurfaceGlass,
                unfocusedContainerColor = SurfaceGlass,
                focusedIndicatorColor = NeonCyan,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = MaterialTheme.shapes.medium,
            singleLine = true
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${viewModel.selectedCount} selected",
                color = NeonCyan,
                fontSize = 14.sp
            )
            Row {
                TextButton(onClick = { viewModel.selectAll() }) {
                    Text("Select All", color = NeonCyan)
                }
                TextButton(onClick = { viewModel.clearSelection() }) {
                    Text("Clear", color = TextSecondary)
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NeonCyan)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredContacts) { contact ->
                    ContactRow(
                        contact = contact,
                        onToggle = { viewModel.toggleContact(contact.normalizedNumber) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.saveSelectedToAllowList() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonCyan,
                contentColor = Color.Black
            ),
            enabled = !isLoading
        ) {
            Text("Import & Continue")
        }
    }
}

@Composable
fun ContactRow(contact: ContactItem, onToggle: () -> Unit) {
    Surface(
        onClick = onToggle,
        shape = MaterialTheme.shapes.medium,
        color = SurfaceGlass,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.displayName,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = contact.phoneNumber,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Checkbox(
                checked = contact.isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = NeonCyan,
                    uncheckedColor = TextSecondary,
                    checkmarkColor = Color.Black
                )
            )
        }
    }
}

@Composable
fun SourcesSelectionStep(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Source Configuration",
            color = TextPrimary,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("risk") }) {
            Text("Continue")
        }
    }
}

@Composable
fun RiskThresholdStep(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Risk Profile Set",
            color = TextPrimary,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { /* Finish onboarding — Step 1.4 */ }) {
            Text("Finish Setup")
        }
    }
}
