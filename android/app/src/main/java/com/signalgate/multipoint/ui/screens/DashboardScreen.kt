package com.signalgate.multipoint.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.signalgate.multipoint.database.entities.SourceEntity
import com.signalgate.multipoint.ui.components.ShieldStatusGlow
import com.signalgate.multipoint.ui.dashboard.DashboardViewModel
import com.signalgate.multipoint.ui.theme.*
import org.koin.androidx.compose.koinViewModel
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OperationalDashboard(
    viewModel: DashboardViewModel = koinViewModel(),
    onOpenDrawer: () -> Unit = {},
    onAddSource: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLaunchOnboarding: () -> Unit = {}
) {
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
    }
    
    LaunchedEffect(Unit) {
        val isFirstLaunch = sharedPreferences.getBoolean("is_first_launch", true)
        if (isFirstLaunch) {
            onLaunchOnboarding()
            sharedPreferences.edit().putBoolean("is_first_launch", false).apply()
        }
    }

    val totalSources by viewModel.totalSources.collectAsState(initial = 0)
    val totalEntries by viewModel.totalEntries.collectAsState(initial = 0)
    val blockedToday by viewModel.blockedToday.collectAsState()
    val dataSources by viewModel.dataSources.collectAsState(initial = emptyList())
    val enabledCount by viewModel.enabledSourcesCount.collectAsState(initial = 0)
    val isSyncing by viewModel.isSyncing.collectAsState()

    val lastSyncTime = if (dataSources.isEmpty()) "Never"
    else formatLastSync(dataSources.maxOfOrNull { it.lastSynced } ?: 0)

    // Responsive breakpoint — portrait phones are typically under 600dp wide
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val isPortrait = screenWidth < 600

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceBackground)
    ) {
        // ── Header ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hamburger
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Menu",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // SG logo box
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(SurfaceGlass, RoundedCornerShape(8.dp))
                    .border(1.dp, NeonCyan, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("SG", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(8.dp))

            // App title
            Column {
                Text(
                    text = "SIGNALGATE",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "MULTI-PORT",
                    color = NeonCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Shield status — scaled down in portrait so it fits
            ShieldStatusGlow(
                statusText = "ACTIVE",
                glowColor = NeonGreen,
                modifier = if (isPortrait) Modifier.scale(0.75f) else Modifier
            )
        }

        HorizontalDivider(color = BorderGlass, thickness = 1.dp)

        // ── Scrollable body ───────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {

            // ── Stats — 2x2 grid in portrait, 1x4 row in landscape ───────────
            item {
                if (isPortrait) {
                    // 2x2 grid — no word wrapping
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DashStatCard(
                                label = "Total Sources",
                                value = totalSources.toString(),
                                subLabel = "Enabled: $enabledCount",
                                modifier = Modifier.weight(1f)
                            )
                            DashStatCard(
                                label = "Total Entries",
                                value = formatLargeNumber(totalEntries),
                                subLabel = "Across all sources",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DashStatCard(
                                label = "Last Sync",
                                value = lastSyncTime,
                                subLabel = if (dataSources.isEmpty()) "No sources" else "All sources OK",
                                modifier = Modifier.weight(1f)
                            )
                            DashStatCard(
                                label = "Blocked Today",
                                value = blockedToday.toString(),
                                subLabel = "+0 vs yesterday",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    // 1x4 row — landscape / tablet
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DashStatCard(
                            label = "Total Sources",
                            value = totalSources.toString(),
                            subLabel = "Enabled: $enabledCount",
                            modifier = Modifier.weight(1f)
                        )
                        DashStatCard(
                            label = "Total Entries",
                            value = formatLargeNumber(totalEntries),
                            subLabel = "Across all sources",
                            modifier = Modifier.weight(1f)
                        )
                        DashStatCard(
                            label = "Last Sync",
                            value = lastSyncTime,
                            subLabel = if (dataSources.isEmpty()) "No sources" else "All sources OK",
                            modifier = Modifier.weight(1f)
                        )
                        DashStatCard(
                            label = "Blocked Today",
                            value = blockedToday.toString(),
                            subLabel = "+0 vs yesterday",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Data Sources header row ───────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DATA SOURCES",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = onAddSource,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp),
                        border = ButtonDefaults.outlinedButtonBorder
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Source",
                            tint = NeonCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Add Source", color = NeonCyan, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = { viewModel.syncAllSources() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                color = DeepSpaceBackground,
                                modifier = Modifier.size(11.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Sync All",
                                tint = DeepSpaceBackground,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Sync All Now", color = DeepSpaceBackground, fontSize = 11.sp)
                    }
                }
            }

            // ── Source cards or empty state ───────────────────────────────────
            if (dataSources.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No data sources yet",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap + Add Source to get started",
                                color = NeonCyan,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                items(dataSources) { source ->
                    SourceCard(
                        source = source,
                        onToggle = { viewModel.toggleSourceEnabled(source.id, !source.isEnabled) },
                        onSync = { viewModel.syncSource(source.id) }
                    )
                }
            }

            // ── Footer ────────────────────────────────────────────────────────
            item {
                HorizontalDivider(color = BorderGlass, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FooterCard(
                        label = "SYNC SCHEDULE",
                        value = "Every 1 hour",
                        subValue = "Next sync soon",
                        modifier = Modifier.weight(1f)
                    )
                    FooterCard(
                        label = "DATABASE HEALTH",
                        value = "Good",
                        subValue = "Optimized",
                        valueColor = NeonGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ── Stat Card ─────────────────────────────────────────────────────────────────

@Composable
fun DashStatCard(
    label: String,
    value: String,
    subLabel: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x1A00E5FF), Color(0x0A0B121A))
                )
            )
            .border(1.dp, BorderGlass, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = NeonCyan,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        if (subLabel.isNotBlank()) {
            Text(
                text = subLabel,
                color = TextSecondary,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

// ── Source Card ───────────────────────────────────────────────────────────────

@Composable
fun SourceCard(
    source: SourceEntity,
    onToggle: () -> Unit,
    onSync: () -> Unit
) {
    val healthColor = when (source.healthStatus) {
        "HEALTHY" -> NeonGreen
        "ERROR"   -> NeonRed
        else      -> TextSecondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceGlass)
            .border(1.dp, BorderGlass, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Health dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(healthColor, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))

        // Name + type
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source.name,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = source.type,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Entries + health status
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatLargeNumber(source.entriesCount),
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = source.healthStatus,
                color = healthColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Toggle
        Switch(
            checked = source.isEnabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = DeepSpaceBackground,
                checkedTrackColor = NeonCyan,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = BorderGlass
            ),
            modifier = Modifier.scale(0.75f)
        )

        // Sync icon
        IconButton(
            onClick = onSync,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Sync source",
                tint = NeonCyan,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ── Footer Card ───────────────────────────────────────────────────────────────

@Composable
fun FooterCard(
    label: String,
    value: String,
    subValue: String,
    valueColor: Color = TextPrimary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceGlass)
            .border(1.dp, BorderGlass, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subValue,
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatLastSync(timestamp: Long): String {
    if (timestamp == 0L) return "Never"
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L     -> "Just now"
        diff < 3_600_000L  -> (diff / 60_000).toString() + "m ago"
        diff < 86_400_000L -> (diff / 3_600_000).toString() + "h ago"
        else               -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun formatLargeNumber(n: Int): String = when {
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
    n >= 1_000     -> String.format("%.1fK", n / 1_000.0)
    else           -> n.toString()
}
