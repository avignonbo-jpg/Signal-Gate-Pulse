package com.signalgate.multipoint.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SignalGateColorScheme = darkColorScheme(
    primary = NeonCyan,
    background = DeepSpaceBackground,
    surface = SurfaceGlass,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun SignalGateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SignalGateColorScheme,
        content = content
    )
}