package com.baz.controller.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baz.controller.ui.theme.AccentAmber
import com.baz.controller.ui.theme.DarkBorder
import com.baz.controller.ui.theme.DarkSurface

@Composable
fun DPad(
    modifier: Modifier = Modifier,
    onDirectionPressed: (up: Boolean, down: Boolean, left: Boolean, right: Boolean) -> Unit
) {
    Column(
        modifier = modifier.aspectRatio(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // UP
        DPadButton(
            text = "▲",
            modifier = Modifier.size(36.dp),
            onClick = { onDirectionPressed(true, false, false, false) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT
            DPadButton(
                text = "◀",
                modifier = Modifier.size(36.dp),
                onClick = { onDirectionPressed(false, false, true, false) }
            )
            // Center Deadzone
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(0xFF0F1522))
            )
            // RIGHT
            DPadButton(
                text = "▶",
                modifier = Modifier.size(36.dp),
                onClick = { onDirectionPressed(false, false, false, true) }
            )
        }

        // DOWN
        DPadButton(
            text = "▼",
            modifier = Modifier.size(36.dp),
            onClick = { onDirectionPressed(false, true, false, false) }
        )
    }
}

@Composable
private fun DPadButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = AccentAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
