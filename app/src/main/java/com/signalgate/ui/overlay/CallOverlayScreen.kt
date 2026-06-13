package com.signalgate.ui.overlay

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

@Composable
fun PulseCallOverlay(
    phoneNumber: String,
    riskScore: Int,
    onAllow: () -> Unit,
    onBlock: () -> Unit
) {
    val glowColor = when {
        riskScore > 80 -> Color.Red
        riskScore > 50 -> Color.Orange
        else -> Color.Green
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                renderEffect = BlurEffect(20f, 20f) // Frosted glass
                shadowElevation = 8f
            }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                // Glowing SG Shield (use Image or Canvas with animated glow)
                Text("🔰 SIGNALGATE", style = MaterialTheme.typography.headlineMedium, color = glowColor)
                Text("Incoming Call: $phoneNumber", style = MaterialTheme.typography.titleLarge)
                Text("Risk: $riskScore% Likely Spam", color = glowColor)

                Spacer(modifier = Modifier.height(32.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = onAllow, colors = ButtonDefaults.buttonColors(Color.Green)) {
                        Text("ALLOW")
                    }
                    Button(onClick = onBlock, colors = ButtonDefaults.buttonColors(Color.Red)) {
                        Text("BLOCK")
                    }
                }
            }
        }
    }
}
