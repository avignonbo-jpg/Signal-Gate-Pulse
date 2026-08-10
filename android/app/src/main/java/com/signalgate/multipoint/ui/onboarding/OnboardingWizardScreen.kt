package com.signalgate.multipoint.ui.onboarding

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.navigation.compose.rememberNavController
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
 * 0a. EULA — binding agreement, must accept to proceed (unnumbered — precedes setup)
 * 0b. Welcome — confirms Pulse flavor, brief SignalGate Trinity mention (unnumbered)
 * 1. Permissions — Request CALL_SCREENING role and required runtime permissions
 * 2. Contacts — Import and select contacts for auto-allow (whitelist)
 * 3. Sources — Select protection level / data sources
 * 4. Complete — Confirm setup and navigate to dashboard
 *
 * Step 0.1 (2026-07-02): RiskThresholdStep now persists onboarding_complete flag.
 * Users will not see this wizard on subsequent app launches.
 *
 * EULA/Welcome added [current session]: unnumbered preamble ahead of the numbered
 * "Step X of 3" flow (Permissions/Contacts/Sources) — deliberately excluded from that
 * counter, same treatment as the unnumbered Completion step, so the count keeps
 * meaning "configuration steps remaining" rather than "screens remaining."
 */

@Composable
fun OnboardingWizardScreen(
    navController: NavHostController,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    // `navController` here is the OUTER, app-level controller passed down from
    // SignalGateNavGraph — it's already attached to that outer NavHost. A
    // NavHostController can only be attached to one NavHost at a time, so
    // reusing it below for this wizard's own NavHost caused:
    // "ViewModelStore should be set before setGraph call".
    //
    // Fix: give the wizard its own internal controller for step-to-step
    // navigation, and only hand the outer `navController` to the final step,
    // which is the one place that needs to navigate OUT to the outer graph's
    // "consumer_dashboard" destination.
    val wizardNavController = rememberNavController()

    NavHost(navController = wizardNavController, startDestination = "eula") {
        composable("eula")        { EulaStep(wizardNavController) }
        composable("welcome")     { WelcomeStep(wizardNavController) }
        composable("permissions") { PermissionsStep(wizardNavController, viewModel) }
        composable("contacts")    { ContactsImportStep(wizardNavController) }
        composable("sources")     { SourcesSelectionStep(wizardNavController, viewModel) }
        composable("risk")        { RiskThresholdStep(navController, viewModel) }
    }
}

// ── STEP 0a: EULA ──────────────────────────────────────────────────────────────

/**
 * EulaStep — Binding agreement gate. First thing any user sees.
 *
 * IMPORTANT: the agreement text below is a structural placeholder only — it is
 * NOT reviewed or approved legal language and must not ship as-is. Get actual
 * terms from counsel before release. What's real here: the acceptance mechanic
 * (scroll-then-checkbox, persisted with a version string so a future terms
 * change can force re-acceptance without re-running the whole wizard).
 */
@Composable
fun EulaStep(navController: NavHostController) {
    val context = LocalContext.current
    var agreed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceBackground)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "License Agreement",
            color = NeonCyan,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceGlass)
                .border(1.dp, BorderGlass, RoundedCornerShape(16.dp))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                item {
                    // PLACEHOLDER — see doc comment above. Not legal-reviewed text.
                    Text(
                        text = "SIGNALGATE PULSE — END USER LICENSE AGREEMENT (PLACEHOLDER)\n\n" +
                            "This placeholder stands in for the binding terms governing use of " +
                            "SignalGate Pulse, including acceptable use, call-screening data " +
                            "handling, and limitations of liability. Replace with reviewed legal " +
                            "text before any release build.\n\n" +
                            "By continuing, you will confirm your agreement to the final terms " +
                            "once they are in place.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = agreed,
                onCheckedChange = { agreed = it },
                colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
            )
            Text(
                text = "I have read and agree to the terms above.",
                color = TextPrimary,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Persisted separately from onboarding_complete, on purpose: this is a
                // legal acceptance record (what was agreed to, and when), not a wizard
                // progress flag. Keeping it distinct means a future terms version bump
                // can require re-acceptance without forcing a full onboarding re-run.
                // Same raw-SharedPreferences approach as RiskThresholdStep's
                // onboarding_complete write — see that step's PULSE-TODO re: migrating
                // both to SettingEntry together rather than diverging further.
                try {
                    val prefs = context.getSharedPreferences(
                        "${context.packageName}_preferences",
                        Context.MODE_PRIVATE
                    )
                    prefs.edit()
                        .putBoolean("eula_accepted", true)
                        .putString("eula_version", "placeholder-v0")
                        .putLong("eula_accepted_at", System.currentTimeMillis())
                        .apply()
                } catch (e: Exception) {
                    Timber.tag("OnboardingWizard").e(e, "Failed to persist EULA acceptance")
                }
                navController.navigate("welcome")
            },
            enabled = agreed,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(1.5.dp, NeonCyan, RoundedCornerShape(28.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonCyan.copy(alpha = 0.2f),
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("I AGREE  ›", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ── STEP 0b: Welcome ───────────────────────────────────────────────────────────

/**
 * WelcomeStep — Introduces the SignalGate suite, confirms the Pulse flavor,
 * and sets expectations: one-time setup, then the app gets out of the way.
 *
 * The Trinity mention here is deliberately one line, not a feature tour of
 * Multi-Port or the Enterprise edition — this is Pulse's first-run moment,
 * not a cross-sell. Fuller Trinity detail belongs in Settings → About.
 */
@Composable
fun WelcomeStep(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceBackground)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Scrollable content lives in a weighted Box (same pattern as EulaStep's
        // LazyColumn) so the CTA button below stays pinned and reachable, while the
        // content itself can scroll on short screens instead of overflowing.
        // Note: a verticalScroll Column and a weight(1f) child can't coexist in the
        // same Column — scroll gives unbounded height, weight needs bounded height —
        // so the scrollable region has to be isolated in its own weighted Box like this.
        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_signal_gate_logo),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "WELCOME TO",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "SIGNALGATE PULSE",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Pulse is part of the SignalGate Trinity — built for set-and-forget " +
                        "protection that pulses to life exactly when you need it.",
                    color = NeonCyan,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(32.dp))

                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.shield_logo),
                    contentDescription = "Shield",
                    modifier = Modifier.size(160.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "A one-time setup, then Pulse works quietly in the background — " +
                        "fewer bogus interruptions, without you having to manage a thing.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Button(
            onClick = { navController.navigate("permissions") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(listOf(NeonCyan, AccentPrimary)),
                    shape = RoundedCornerShape(28.dp)
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonCyan.copy(alpha = 0.2f),
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("BEGIN  ›", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
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

    val roleLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

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
        // Scrollable content in a weighted Box, CTA pinned below — same reasoning
        // as WelcomeStep (verticalScroll and weight(1f) can't share a Column).
        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
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
            }
        }

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
                    !roleHeld -> {
                        val roleManager =
                            context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                        roleLauncher.launch(
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
 * - "Skip" advances with neither allow nor block entries created — this step
 *   only ever grants ALLOW, never BLOCK, so there's no bulk-selection path that
 *   can isolate a user from their own contacts.
 * - Advances to sources step when saved (including the zero-selected and skip
 *   cases — previously, selecting zero contacts silently dead-ended the wizard
 *   with no way to proceed).
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
    val saveError by viewModel.saveError.collectAsState()
    // Bug fix: viewModel.filteredContacts / viewModel.selectedCount are plain getters
    // that read the raw StateFlow.value directly, bypassing Compose's snapshot-read
    // tracking entirely. Since `contacts` above is never otherwise read in this
    // composable, Compose had no dependency to recompose on — so selectAll()/
    // clearSelection()/toggleContact() updated the ViewModel's data but the LazyColumn
    // (and the "N selected" count) never redrew, which is why Select All looked like
    // it did nothing. Deriving both from the already-collected `contacts` state fixes
    // it: now they're genuine Compose State reads that trigger recomposition.
    val filteredContacts = remember(contacts, searchQuery) {
        val query = searchQuery.trim().lowercase()
        if (query.isEmpty()) contacts
        else contacts.filter {
            it.displayName.lowercase().contains(query) || it.phoneNumber.contains(query)
        }
    }
    val selectedCount = remember(contacts) { contacts.count { it.isSelected } }

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
            Text("$selectedCount selected", color = NeonCyan, fontSize = 13.sp)
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

        if (saveError != null) {
            Text(
                text = saveError ?: "",
                color = Color(0xFFFF6B6B),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

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

        TextButton(
            onClick = { viewModel.skipContactImport() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(
                "Skip — I'll manage this later",
                color = TextSecondary,
                fontSize = 13.sp
            )
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
 * SourcesSelectionStep — Third step: on-device gray-zone heuristics protection
 * level. This is real, not a placeholder: CallRiskEvaluator (the STIR/SHAKEN +
 * source-match gray-zone scorer) is already built and already wired into
 * CallScreeningEngine at the Tier 4/5 boundary — see CallScreeningEngine's doc
 * comment. What was missing was a user-facing control for it; previously the
 * risk threshold was a hardcoded constant. This screen sets HeuristicsMode,
 * persisted immediately (not deferred to wizard completion) via
 * OnboardingViewModel.setHeuristicsMode(), which CallScreeningEngine now reads
 * on every call. See SettingKeys.kt's HeuristicsMode enum for exactly what
 * number backs each level.
 *
 * @param navController Navigation controller for step progression
 * @param viewModel OnboardingViewModel — same instance the rest of the wizard uses
 */
@Composable
fun SourcesSelectionStep(navController: NavHostController, viewModel: OnboardingViewModel) {
    val selectedMode by viewModel.heuristicsMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StepIndicator(currentStep = 3, totalSteps = 3)
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Step 3: Protection Level",
                    color = NeonCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "On-device heuristics review borderline calls that don't match a " +
                        "known list — no data ever leaves your phone. Choose how " +
                        "sensitive that review should be.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                HeuristicsMode.entries.forEach { mode ->
                    ProtectionLevelOption(
                        mode = mode,
                        selected = mode == selectedMode,
                        onSelect = { viewModel.setHeuristicsMode(mode) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
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

/**
 * ProtectionLevelOption — single selectable protection-level card for Step 3.
 */
@Composable
private fun ProtectionLevelOption(
    mode: HeuristicsMode,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val subtitle = when (mode) {
        HeuristicsMode.OFF ->
            "Heuristics disabled. Only known allow/block-list matches are screened."
        HeuristicsMode.CONSERVATIVE ->
            "Flags only the highest-risk borderline calls. Fewest false positives."
        HeuristicsMode.BALANCED ->
            "Recommended default. A reasonable balance of coverage and accuracy."
        HeuristicsMode.AGGRESSIVE ->
            "Flags more borderline calls for review. Catches more spam, at the cost of more false positives."
    }
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) NeonCyan.copy(alpha = 0.14f) else SurfaceGlass,
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) NeonCyan else BorderGlass
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    mode.label,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (selected) NeonCyan else Color.Transparent)
                    .border(1.5.dp, if (selected) NeonCyan else TextSecondary, CircleShape)
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ── STEP 4: Completion ─────────────────────────────────────────────────────────

/**
 * RiskThresholdStep — Final step: Confirm setup and navigate to dashboard.
 *
 * Step 2.6: onboarding_complete now migrated to SettingEntry via
 * OnboardingViewModel.markOnboardingComplete(), replacing the direct
 * SharedPreferences write this step used to make. Follows the same
 * screen-observes-ViewModel-state pattern as ContactsImportStep's isSaved:
 * this Composable calls markOnboardingComplete() and reacts to
 * onboardingCompleted via LaunchedEffect — it never touches SettingRepository,
 * or persistence of any kind, directly.
 *
 * @param navController Navigation controller for final navigation — this is
 *   deliberately the OUTER app-level controller (see OnboardingWizardScreen's
 *   doc comment), since this is the one step that navigates out of the wizard.
 * @param viewModel OnboardingViewModel — same instance the rest of the wizard uses.
 */
@Composable
fun RiskThresholdStep(navController: NavHostController, viewModel: OnboardingViewModel) {
    val context = LocalContext.current
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()

    // Both completion buttons persist onboarding_complete the same way — they only
    // differ in what happens once that's confirmed saved. This flag records which
    // one the user tapped so the single onboardingCompleted LaunchedEffect below
    // knows whether to navigate into the dashboard UI or just background the app.
    var landingAction by remember { mutableStateOf<CompletionAction?>(null) }

    LaunchedEffect(onboardingCompleted) {
        if (onboardingCompleted) {
            Timber.tag("OnboardingWizard").i("Onboarding marked complete")
            when (landingAction) {
                CompletionAction.PULSE_MODE -> {
                    // Pulse Mode: set-and-forget call blocking runs quietly in the
                    // background; the user is only pulled back in via the pulsed-
                    // vibration notification when a call needs their input. So this
                    // doesn't navigate anywhere in-app — it backgrounds the whole
                    // task, same as pressing Home, leaving the wizard/dashboard UI
                    // dismissed rather than swapped for another screen.
                    (context as? Activity)?.moveTaskToBack(true)
                }
                else -> {
                    // Routes must match Screen.kt's registered route strings exactly —
                    // "dashboard" / "onboarding" (Screen.Dashboard.route / Screen.Onboarding.route),
                    // NOT "consumer_dashboard" / "onboarding_wizard". Navigating to a route that
                    // isn't registered in SignalGateNavGraph throws immediately, which is what was
                    // crashing the app on "Go To Dashboard" — this is the outer app-level
                    // navController (see OnboardingWizardScreen's doc comment), so it resolves
                    // against SignalGateNavGraph's routes, not this wizard's internal ones.
                    navController.navigate("dashboard") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceBackground)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
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

        Button(
            onClick = {
                landingAction = CompletionAction.DASHBOARD
                viewModel.markOnboardingComplete()
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

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                landingAction = CompletionAction.PULSE_MODE
                viewModel.markOnboardingComplete()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(1.dp, BorderGlass, RoundedCornerShape(28.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = SurfaceGlass
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                "GO TO PULSE MODE  ›",
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Pulse Mode runs quietly in the background — we'll pulse-alert you only when a call needs your decision.",
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

private enum class CompletionAction { DASHBOARD, PULSE_MODE }
