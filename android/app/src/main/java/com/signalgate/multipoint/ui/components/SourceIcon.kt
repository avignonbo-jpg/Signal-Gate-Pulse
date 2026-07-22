package com.signalgate.multipoint.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.signalgate.multipoint.data.models.SourceType

@Composable
fun SourceIcon(
    sourceType: SourceType,
    modifier: Modifier = Modifier
) {
    val (iconVector, backgroundColor, iconColor) = when (sourceType) {
        SourceType.REMOTE_URL -> Triple(Icons.Default.LocationOn, Color(0x1A00E676), Color(0xFF00E676)) // Green/Globe accent
        SourceType.LOCAL_CSV -> Triple(Icons.Default.List, Color(0x1A00E5FF), Color(0xFF00E5FF))      // Cyan/Document accent
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .background(color = backgroundColor, shape = RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
    }
}
