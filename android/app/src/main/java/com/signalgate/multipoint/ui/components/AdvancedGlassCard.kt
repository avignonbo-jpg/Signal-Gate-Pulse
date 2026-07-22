package com.signalgate.multipoint.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.signalgate.multipoint.ui.theme.BorderGlass
import com.signalgate.multipoint.ui.theme.SurfaceGlass

@Composable
fun AdvancedGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            // Layered alpha values guarantee readability over any dark wallpaper/background asset
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SurfaceGlass.copy(alpha = 0.25f),
                        SurfaceGlass.copy(alpha = 0.12f)
                    )
                )
            )
            // Simulates light catching the top/left glassy edges perfectly matching mockups
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        BorderGlass,
                        Color.Transparent,
                        BorderGlass.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            content()
        }
    }
}
