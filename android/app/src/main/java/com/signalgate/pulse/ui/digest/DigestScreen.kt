package com.signalgate.pulse.ui.digest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.signalgate.pulse.database.entities.PendingCardEntity
import com.signalgate.pulse.ui.theme.*
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * DigestScreen — the blocked call review queue (Screen.Digest).
 *
 * Reached via:
 *   - Notification content tap → signalgate://digest deep link → NavGraph
 *   - "View Recent Activity" on the consumer dashboard
 *
 * Each card represents one Tier 3 HEURISTIC_BLOCK decision that the user
 * has not yet reviewed. Two actions per card:
 *   "Not Spam" — overturn: allowlists the number, removes from queue.
 *   "Dismiss"  — acknowledges: removes from queue without allowlisting.
 *
 * "Dismiss All" clears the entire queue.
 *
 * Swipe-to-dismiss is intentionally omitted for now — the two inline
 * buttons are clearer for non-technical users. Add swipe in a future
 * release once the gesture is validated with real users.
 *
 * Package: com.signalgate.pulse.ui.digest (alongside PendingCardViewModel)
 */
@Composable
fun DigestScreen(
    modifier: Modifier = Modifier,
    viewModel: PendingCardViewModel = koinViewModel()
) {
    val cards by viewModel.undismissedCards.collectAsState(initial = emptyList())
    val cardCount by viewModel.undismissedCount.collectAsState(initial = 0)
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpaceBackground)
            .padding(16.dp)
    ) {

        // ── Header ──────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "BLOCKED CALLS",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (cardCount == 0) "All clear" else "$cardCount awaiting review",
                    color = if (cardCount == 0) NeonGreen else NeonOrange,
                    fontSize = 13.sp
                )
            }

            if (cardCount > 0) {
                TextButton(
                    onClick = { viewModel.dismissAll() }
                ) {
                    Text(
                        text = "Dismiss All",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        HorizontalDivider(
            color = BorderGlass,
            thickness = 1.dp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // ── Empty state ─────────────────────────────────────────────────────────
        if (cards.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No blocked calls to review",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "SignalGate is running quietly in the background.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {

            // ── Card list ────────────────────────────────────────────────────────
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = cards,
                    key = { it.id }
                ) { card ->
                    BlockedCallCard(
                        card = card,
                        dateFormat = dateFormat,
                        onNotSpam = { viewModel.markAsNotSpam(card) },
                        onDismiss = { viewModel.dismissCard(card.id) }
                    )
                }
            }
        }
    }
}

// ── BlockedCallCard ─────────────────────────────────────────────────────────────

@Composable
private fun BlockedCallCard(
    card: PendingCardEntity,
    dateFormat: SimpleDateFormat,
    onNotSpam: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceGlass,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            // ── Card header ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Shield icon with red tint
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = NeonRed,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.phoneNumber,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = dateFormat.format(Date(card.timestamp)),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                // Confidence badge — only shown when confidence is available
                card.confidence?.let { confidence ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = confidenceBadgeColor(confidence).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "$confidence%",
                            color = confidenceBadgeColor(confidence),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // ── Decision source ──────────────────────────────────────────────────
            card.decisionSource?.let { source ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Matched: $source",
                    color = NeonOrange,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderGlass, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // ── Action buttons ───────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Not Spam — overturn and allowlist
                Button(
                    onClick = onNotSpam,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen.copy(alpha = 0.15f),
                        contentColor = NeonGreen
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Not Spam",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Dismiss — acknowledge without allowlisting
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSecondary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(BorderGlass)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Dismiss",
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────────

private fun confidenceBadgeColor(confidence: Int): Color = when {
    confidence >= 80 -> NeonRed
    confidence >= 50 -> NeonOrange
    else -> TextSecondary
}
