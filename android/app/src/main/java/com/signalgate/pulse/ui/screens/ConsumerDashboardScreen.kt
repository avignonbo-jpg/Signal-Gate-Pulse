package com.signalgate.pulse.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.signalgate.pulse.R
import com.signalgate.pulse.ui.dashboard.DashboardViewModel
import com.signalgate.pulse.ui.theme.*
import org.koin.androidx.compose.koinViewModel

/**
 * ConsumerDashboard — the default home screen for Pulse users.
 *
 * Visual spec from approved mockup:
 *   - Top: logo wordmark left, three-dot menu right
 *   - Hero: glassmorphic card, glowing shield asset, SHIELD ACTIVE/INACTIVE,
 *           subtitle "Your protection is running in the background."
 *   - Two stat cards: Calls Screened Today + Threats Blocked with cyan counts
 *   - SETTINGS pill button with cyan border glow
 *   - "View Recent Activity ›" text link → Screen.Digest
 *
 * Phase 2 Step 2.2. wired to real DashboardViewModel state (Steps 2.3–2.5).
 * startDestination in NavGraph replaces OperationalDashboard for Pulse users.
 */
@Composable
fun ConsumerDashboardScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    onLaunchOnboarding: () -> Unit = {},
    viewModel: DashboardViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val shieldActive by viewModel.shieldActive.collectAsState()
    val blockedToday by viewModel.blockedToday.collectAsState()
    val callsScreened by viewModel.callsScreenedToday.collectAsState(initial = 0)

    // First-launch → onboarding routing, ported from the retired OperationalDashboard.
    // Step 2.6: now reads isOnboardingComplete from DashboardViewModel (backed by
    // SettingRepository) instead of SharedPreferences directly — this Composable no
    // longer touches persistence at all. OnboardingViewModel.markOnboardingComplete()
    // is the write side; both moved to SettingEntry together, as planned.
    //
    // Nullable on purpose: null means "still loading," and must never be treated as
    // "not complete" — see DashboardViewModel's doc comment on this flow.
    val isOnboardingComplete by viewModel.isOnboardingComplete.collectAsState()
    LaunchedEffect(isOnboardingComplete) {
        if (isOnboardingComplete == false) {
            onLaunchOnboarding()
        }
    }

    // Recheck role on every resume — never cached
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkShieldStatus(context)
                viewModel.refreshCounters()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceBackground)
    ) {

        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo + wordmark
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_signal_gate_logo),
                    contentDescription = "SignalGate logo",
                    modifier = Modifier.size(36.dp)
                )
                Column {
                    Text(
                        text = "SIGNAL GATE",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                    Text(
                        text = "PULSE",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = { /* future: settings menu */ }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = TextSecondary
                )
            }
        }

        // ── Scrollable content ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Hero shield card ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                GlassSurface.copy(alpha = 0.7f),
                                SurfaceDark.copy(alpha = 0.5f)
                            )
                        )
                    )
                    .border(1.dp, BorderGlass, RoundedCornerShape(20.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Glowing shield asset — pre-rendered PNG with baked glow
                    Image(
                        painter = painterResource(R.drawable.shield_logo),
                        contentDescription = "Shield",
                        modifier = Modifier.size(160.dp)
                    )

                    Text(
                        text = if (shieldActive) "SHIELD ACTIVE" else "SHIELD INACTIVE",
                        color = if (shieldActive) TextPrimary else NeonRed,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = if (shieldActive)
                            "Your protection is running\nin the background."
                        else
                            "Call screening role not granted.\nTap to restore protection.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── Stat cards ───────────────────────────────────────────────────
            StatCard(
                icon = Icons.Default.Phone,
                label = "Calls Screened Today:",
                count = callsScreened
            )

            StatCard(
                icon = Icons.Default.Shield,
                label = "Threats Blocked:",
                count = blockedToday
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Settings button ──────────────────────────────────────────────
            Button(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(
                        width = 1.5.dp,
                        color = NeonCyan,
                        shape = RoundedCornerShape(28.dp)
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceDark.copy(alpha = 0.6f),
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "SETTINGS",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = TextPrimary
                )
            }

            // ── View Recent Activity link ────────────────────────────────────
            TextButton(
                onClick = onNavigateToActivity,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "View Recent Activity  ›",
                    color = NeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ── StatCard ──────────────────────────────────────────────────────────────────

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        GlassSurface.copy(alpha = 0.6f),
                        SurfaceDark.copy(alpha = 0.4f)
                    )
                )
            )
            .border(1.dp, BorderGlass, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(AccentPrimary.copy(alpha = 0.15f))
                .border(1.dp, AccentPrimary.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = label,
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        // Vertical divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(32.dp)
                .background(BorderGlass)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = count.toString(),
            color = NeonCyan,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Composable Image wrapper (avoids import conflict with Material Icon) ───────

@Composable
private fun Image(
    painter: androidx.compose.ui.graphics.painter.Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier
    )
}
