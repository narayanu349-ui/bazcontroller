package com.baz.controller.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baz.controller.ui.theme.AccentAmber
import com.baz.controller.ui.theme.AccentCyan
import com.baz.controller.ui.theme.AccentGreen
import com.baz.controller.ui.theme.DarkBorder
import com.baz.controller.ui.theme.DarkSurface

@Composable
fun DashboardCluster(
    speedKmh: Int,
    rpmPercent: Float,
    indicatorLeft: Boolean,
    indicatorRight: Boolean,
    hazards: Boolean,
    highBeam: Boolean,
    currentGear: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left Turn Indicator
        Text(
            text = "◀",
            color = if (indicatorLeft || hazards) AccentGreen else Color(0xFF374151),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        // Speed Display
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format("%03d", speedKmh),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "KM/H",
                color = Color(0xFF9CA3AF),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Active Gear
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1B2335))
                .border(1.dp, AccentAmber.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (currentGear) {
                    -1 -> "R"
                    0 -> "N"
                    else -> "$currentGear"
                },
                color = AccentAmber,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }

        // RPM Gauge & High Beam
        Column(
            modifier = Modifier.width(70.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("RPM", color = Color(0xFF9CA3AF), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                Text(
                    text = "HIGH",
                    color = if (highBeam) AccentCyan else Color(0xFF374151),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF1E2638))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(rpmPercent.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(if (rpmPercent > 0.85f) Color(0xFFEF4444) else AccentAmber)
                )
            }
        }

        // Right Turn Indicator
        Text(
            text = "▶",
            color = if (indicatorRight || hazards) AccentGreen else Color(0xFF374151),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
