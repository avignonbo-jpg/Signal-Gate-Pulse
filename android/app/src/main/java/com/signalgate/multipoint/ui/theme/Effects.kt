package com.signalgate.multipoint.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.glassPanel() = this
    .clip(RoundedCornerShape(24.dp))
    .background(
        Brush.verticalGradient(
            listOf(
                GlassSurface.copy(alpha = 0.60f),
                SurfaceDark.copy(alpha = 0.40f)
            )
        )
    )
    .border(
        width = 1.dp,
        color = GlassBorder,
        shape = RoundedCornerShape(24.dp)
    )

fun Modifier.glassPanelSmall() = this
    .clip(RoundedCornerShape(16.dp))
    .background(
        Brush.verticalGradient(
            listOf(
                GlassSurface.copy(alpha = 0.50f),
                SurfaceDark.copy(alpha = 0.35f)
            )
        )
    )
    .border(
        width = 1.dp,
        color = GlassBorder,
        shape = RoundedCornerShape(16.dp)
    )

object SignalGateGlow {

    val Blue = AccentPrimary.copy(alpha = 0.35f)

    val Green = StatusActive.copy(alpha = 0.35f)

    val Red = StatusHigh.copy(alpha = 0.35f)

    val Orange = StatusMedium.copy(alpha = 0.35f)
}
