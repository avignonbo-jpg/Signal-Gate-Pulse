package com.signalgate.multipoint.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.signalgate.multipoint.ui.theme.SignalGateGlow
import com.signalgate.multipoint.ui.theme.glassPanel

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    glowColor: Color = SignalGateGlow.Blue,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = glowColor,
                spotColor = glowColor
            )
            .glassPanel()
            .padding(16.dp)
    ) {
        content()
    }
}
