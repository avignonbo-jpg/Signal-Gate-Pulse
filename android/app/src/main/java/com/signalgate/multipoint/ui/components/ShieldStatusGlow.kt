package com.signalgate.multipoint.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.signalgate.multipoint.ui.theme.NeonGreen

@Composable
fun ShieldStatusGlow(
    modifier: Modifier = Modifier,
    statusText: String = "ACTIVE",
    glowColor: Color = NeonGreen
) {
    // Infinite transition for the breathing neon pulse effect
    val infiniteTransition = rememberInfiniteTransition(label = "ShieldPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaPulse"
    )

    Row(
        modifier = modifier
            .wrapContentSize()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(end = 12.dp)
        ) {
            Text(
                text = "Shield Status",
                color = Color(0xFFB0BEC5),
                fontSize = 11.sp
            )
            Text(
                text = statusText,
                color = glowColor,
                fontSize = 16.sp,
                style = androidx.compose.ui.text.TextStyle(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
        }

        // Native Canvas Drawing for the Neon Shield Icon
        Canvas(modifier = Modifier.size(36.dp)) {
            val width = size.width
            val height = size.height

            drawIntoCanvas { canvas ->
                val frameworkPaint = Paint().asFrameworkPaint().apply {
                    color = glowColor.copy(alpha = pulseAlpha).hashCode()
                    // Emulate high-fidelity neon glow using a BlurMaskFilter
                    maskFilter = android.graphics.BlurMaskFilter(15f, android.graphics.BlurMaskFilter.Blur.NORMAL)
                }

                val path = android.graphics.Path().apply {
                    moveTo(width * 0.5f, height * 0.1f)
                    cubicTo(width * 0.8f, height * 0.1f, width * 0.9f, height * 0.2f, width * 0.9f, height * 0.4f)
                    cubicTo(width * 0.9f, height * 0.7f, width * 0.5f, height * 0.95f, width * 0.5f, height * 0.95f)
                    cubicTo(width * 0.5f, height * 0.95f, width * 0.1f, height * 0.7f, width * 0.1f, height * 0.4f)
                    cubicTo(width * 0.1f, height * 0.2f, width * 0.2f, height * 0.1f, width * 0.5f, height * 0.1f)
                    close()
                }

                // Draw background glow layer
                canvas.nativeCanvas.drawPath(path, frameworkPaint)

                // Clean core border layer
                val strokePaint = android.graphics.Paint().apply {
                    color = glowColor.hashCode()
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 4f
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawPath(path, strokePaint)
            }
        }
    }
}
