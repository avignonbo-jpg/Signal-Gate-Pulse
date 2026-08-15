package com.signalgate.pulse.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.signalgate.pulse.ui.theme.AccentPrimary
import com.signalgate.pulse.ui.theme.StatusActive
import com.signalgate.pulse.ui.theme.StatusHigh
import com.signalgate.pulse.ui.theme.StatusMedium

enum class NeonButtonStyle {
    Primary,
    Success,
    Warning,
    Danger
}

@Composable
fun NeonButton(
    text: String,
    modifier: Modifier = Modifier,
    style: NeonButtonStyle = NeonButtonStyle.Primary,
    onClick: () -> Unit
) {

    val color = when (style) {
        NeonButtonStyle.Primary -> AccentPrimary
        NeonButtonStyle.Success -> StatusActive
        NeonButtonStyle.Warning -> StatusMedium
        NeonButtonStyle.Danger -> StatusHigh
    }

    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = color
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(text)
        }
    }
}
