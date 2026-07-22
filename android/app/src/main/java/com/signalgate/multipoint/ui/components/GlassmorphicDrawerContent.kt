package com.signalgate.multipoint.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.signalgate.multipoint.ui.navigation.Screen
import com.signalgate.multipoint.ui.theme.*

@Composable
fun GlassmorphicDrawerContent(
    currentRoute: String?,
    onDestinationSelected: (Screen) -> Unit
) {
    val screens = listOf(
        Screen.Dashboard,
        Screen.Sources,
        Screen.CallLog,
        Screen.BlockAllowList,
        Screen.Settings
    )

    ModalDrawerSheet(
        drawerContainerColor = DeepSpaceBackground,
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(listOf(Color.Transparent, BorderGlass)),
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // App Branding Header matching image 1000016486
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceGlass, shape = RoundedCornerShape(8.dp))
                        .border(1.dp, NeonCyan, shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SG", color = NeonCyan, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("SIGNALGATE", color = TextPrimary, fontSize = 18.sp)
                    Text("MULTI-PORT", color = TextSecondary, fontSize = 12.sp)
                }
            }

            HorizontalDivider(color = BorderGlass, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Items
            screens.forEach { screen ->
                val isSelected = currentRoute == screen.route
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) SurfaceGlass else Color.Transparent)
                        .clickable { onDestinationSelected(screen) }
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) BorderGlass else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.title,
                            tint = if (isSelected) NeonCyan else TextSecondary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = screen.title,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
