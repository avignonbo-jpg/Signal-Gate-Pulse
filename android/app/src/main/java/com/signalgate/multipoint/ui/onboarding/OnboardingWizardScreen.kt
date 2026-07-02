package com.signalgate.multipoint.ui.onboarding

import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.signalgate.multipoint.R
import com.signalgate.multipoint.ui.theme.*
import com.signalgate.multipoint.ui.viewmodels.ContactItem
import com.signalgate.multipoint.ui.viewmodels.ContactsViewModel
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber

/**
 * OnboardingWizardScreen — Multi-step setup flow for new users.
 *
 * Steps:
 * 1. Permissions — Request CALL_SCREENING role and required runtime permissions
 * 2. Contacts — Import and select contacts for auto-allow (whitelist)
 * 3. Sources — Select protection level / data sources
 * 4. Complete — Confirm setup and navigate to dashboard
 *
 * Step 0.1 (2026-07-02): RiskThresholdStep now persists onboarding_complete flag.
 * Users will not see this wizard on subsequent app launches.
 */

@Composable
fun OnboardingWizardScreen(
    navController: NavHostController,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    NavHost(navController = navController, startDestination = "permissions") {
        composable("permissions") { PermissionsStep(navController, viewModel) }
        composable("contacts")    { ContactsImportStep(navController) }
        composable("sources")     { SourcesSelectionStep(navController) }
        composable("risk")        { RiskThresholdStep(navController) }
    }
}

// ── Step progress indicator ────────────────────────────────────────────────────

/**
 * StepIndicator — Visual progress indicator showing current step.
 * Displays text (e.g., "Step 1 of 3") and a dot sequence.
 *
 * @param currentStep Current step number (1-indexed)
 * @param totalSteps Total number of steps
 */
@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceGlass)
            .border(1.dp, BorderGlass, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Step $currentStep of $totalSteps",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.width(4.dp))
        HorizontalDivider(
            modifier = Modifier
                .width(1.dp)
                .height(14.dp),
            color = BorderGlass,
            thickness = 1.dp
        )
        Spacer(modifier = Modifier.width(4.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(totalSteps) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < currentStep) NeonCyan
                            else BorderGlass
                        )
                )
            }
        }
    }
}

// ── STEP 1: Permissions ────────────────────────────────────────────────────────

/**
 * PermissionsStep — First step: Request Call Screening role and runtime permissions.
 *
 * Behavior:
 * - Checks current permission status on every resume
 * - Prompts user to grant READ_CONTACTS and READ_CALL_LOG if needed
 * - Prompts user to grant ROLE_CALL_SCREENING if needed
 * - Advances to contacts step when all permissions are granted
 *
 * @param navController Navigation controller for step progression
 * @param viewModel OnboardingViewModel for permission state management
 */
@Composable
fun PermissionsStep(navController: NavHostController, viewModel: OnboardingViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionStates by viewModel.permissionStates.collectAsState()
    val roleHeld by viewModel.callScreeningRoleHeld.collectAsState()
    var showLearnMore by remember { mutableStateOf(false) }

    val roleLauncher = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    } else null

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        results.forEach { (permission, granted) ->
            viewModel.onPermissionResult(permission, granted)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.updateAllPermissions(
                    viewModel.permissions.associate { item ->
                        item.permission to (ContextCompat.checkSelfPermission(
                            context, item.permission
                        ) == PackageManager.PERMISSION_GRANTED)
                    }
                )
                viewModel.checkCallScreeningRole(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceBackground)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Wordmark
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_signal_gate_logo),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    text = "SIGNAL GATE",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "PULSE",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        StepIndicator(currentStep = 1, totalSteps = 3)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Step 1: Security Foundation",
            color = NeonCyan,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Hero shield — pre-rendered glowing asset
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.shield_logo),
            contentDescription = "Shield",
            modifier = Modifier.size(200.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Permission label row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_signal_gate_logo),
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = "Permission Request: Call Screening",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Grant permission to screen incoming\ncalls for real-time protection.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Start
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // GRANT ACCESS primary CTA
        val allReady = viewModel.allRequiredGranted() && roleHeld
        Button(
            onClick = {
                val ungranted = viewModel.permissions
                    .filter { permissionStates[it.permission] == false }
                    .map { it.permission }
                when {
                    ungranted.isNotEmpty() ->
                        permissionLauncher.launch(ungranted.toTypedArray())
                    !roleHeld && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                        val roleManager =
                            context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                        roleLauncher?.launch(
                            roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                        )
                    }
                    else -> navController.navigate("contacts")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(listOf(NeonCyan, AccentPrimary)),
                    shape = RoundedCornerShape(28.dp)
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (allReady) NeonCyan.copy(alpha = 0.2f)
                else SurfaceDark.copy(alpha = 0.6f),
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(
                imageVector = if (allReady) Icons.Default.CheckCircle else Icons.Default.Lock,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = when {
                    allReady -> "CONTINUE  ›"
                    !viewModel.allRequiredGranted() -> "GRANT ACCESS  ›"
                    else -> "GRANT CALL SCREENING  ›"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Learn More link
        HorizontalDivider(color = BorderGlass.copy(alpha = 0.4f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            TextButton(onClick = { showLearnMore = true }) {
                Text("Learn More", color = NeonCyan, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showLearnMore) {
        AlertDialog(
            onDismissRequest = { showLearnMore = false },
            title = { Text("About Call Screening", color = TextPrimary) },
            text = {
                Text(
                    "SignalGate Pulse uses Android's built-in Call Screening role to " +
                    "analyze incoming calls before your phone rings. This is a system-level " +
                    "permission — only one app can hold it at a time. Your calls are analyzed " +
                    "on-device and never sent to any external server.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showLearnMore = false }) {
                    Text("Got it", color = NeonCyan)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

// ── STEP 2: Contacts ───────────────────────────────────────────────────────────

/**
 * ContactsImportStep — Second step: Select contacts for auto-allow whitelist.
 *
 * Behavior:
 * - Loads device contacts
 * - Allows search and filtering
 * - Multi-select with Select All / Clear buttons
 * - Saves selected contacts to UnifiedEntryEntity with action='ALLOW'
 * - Advances to sources step when saved
 *
 * @param navController Navigation controller for step progression
 */
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

    LaunchedEffect(Unit) { viewModel.loadContacts(context) }
    LaunchedEffect(isSaved) { if (isSaved) navController.navigate("sources") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceBackground)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        StepIndicator(currentStep = 2, totalSteps = 3)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Step 2: Contacts Auto-Allow",
            color = NeonCyan,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Select contacts to automatically allow through the shield.",
            color = TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
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
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${viewModel.selectedCount} selected", color = NeonCyan, fontSize = 13.sp)
            Row {
                TextButton(onClick = { viewModel.selectAll() }) {
                    Text("Select All", color = NeonCyan, fontSize = 12.sp)
                }
                TextButton(onClick = { viewModel.clearSelection() }) {
                    Text("Clear", color = TextSecondary, fontSize = 12.sp)
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

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.saveSelectedToAllowList() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(1.5.dp, NeonCyan, RoundedCornerShape(28.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonCyan.copy(alpha = 0.2f),
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(28.dp),
            enabled = !isLoading
        ) {
            Text("IMPORT & CONTINUE  ›", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * ContactRow — Individual contact selection row.
 *
 * @param contact Contact to display
 * @param onToggle Callback when user toggles selection
 */
@Composable
fun ContactRow(contact: ContactItem, onToggle: () -> Unit) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(12.dp),
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
                Text(contact.displayName, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(contact.phoneNumber, color = TextSecondary, fontSize = 12.sp)
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

// ── STEP 3: Sources ────────────────────────────────────────────────────────────

/**
 * SourcesSelectionStep — Third step: Confirm protection level.
 *
 * Placeholder for future expansion into protection mode selection
 * (Conservative, Balanced, Aggressive).
 *
 * @param navController Navigation controller for step progression
 */
@Composable
fun SourcesSelectionStep(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        StepIndicator(currentStep = 3, totalSteps = 3)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Step 3: Protection Level",
            color = NeonCyan,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate("risk") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(1.5.dp, NeonCyan, RoundedCornerShape(28.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonCyan.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                "CONTINUE  ›",
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextPrimary
            )
        }
    }
}

// ── STEP 4: Completion ─────────────────────────────────────────────────────────

/**
 * RiskThresholdStep — Final step: Confirm setup and navigate to dashboard.
 *
 * Step 0.1 (2026-07-02): Now persists onboarding_complete flag.
 * When user taps "GO TO DASHBOARD", this function:
 * 1. Writes onboarding_complete = true to SharedPreferences
 * 2. Navigates to consumer_dashboard with pop back stack
 * 3. Prevents returning to onboarding wizard on future launches
 *
 * PULSE-TODO (2026-06): Replace SharedPreferences with SettingEntry — Step 2.6.
 *
 * @param navController Navigation controller for final navigation
 */
@Composable
fun RiskThresholdStep(navController: NavHostController) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "You're all set.",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "SignalGate Pulse is now protecting your calls.",
            color = TextSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        /**
         * Step 0.1 (2026-07-02): Persist onboarding_complete flag.
         *
         * Behavior:
         * 1. Mark onboarding as complete in SharedPreferences
         * 2. Log completion to Timber for debugging
         * 3. Navigate to dashboard, clearing back stack
         * 4. User will not see onboarding wizard on subsequent launches
         *
         * PULSE-TODO (2026-06): Migrate to SettingEntry.onboarding_complete after Step 2.6.
         * Current implementation uses SharedPreferences for backward compatibility.
         */
        Button(
            onClick = {
                try {
                    // Mark onboarding as complete
                    val prefs = context.getSharedPreferences(
                        "${context.packageName}_preferences",
                        Context.MODE_PRIVATE
                    )
                    prefs.edit()
                        .putBoolean("onboarding_complete", true)
                        .apply()

                    Timber.tag("OnboardingWizard").i("Onboarding marked complete")

                    // Navigate to dashboard, clearing back stack
                    navController.navigate("consumer_dashboard") {
                        popUpTo("onboarding_wizard") { inclusive = true }
                    }
                } catch (e: Exception) {
                    Timber.tag("OnboardingWizard").e(e, "Failed to complete onboarding")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(1.5.dp, NeonCyan, RoundedCornerShape(28.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonCyan.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                "GO TO DASHBOARD  ›",
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextPrimary
            )
        }
    }
}
