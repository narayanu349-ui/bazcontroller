package com.baz.controller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baz.controller.BazApplication
import com.baz.controller.ui.theme.AccentAmber
import com.baz.controller.ui.theme.AccentCyan
import com.baz.controller.ui.theme.DarkBg
import com.baz.controller.ui.theme.DarkBorder
import com.baz.controller.ui.theme.DarkSurface
import com.baz.controller.ui.theme.TextPrimary

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val haptics = BazApplication.instance.hapticsManager

    var maxWheelDeg by remember { mutableStateOf(360f) }
    var sensitivity by remember { mutableStateOf(1.0f) }
    var deadzone by remember { mutableStateOf(0.02f) }
    var autoCenter by remember { mutableStateOf(true) }
    var hapticsEnabled by remember { mutableStateOf(haptics.isEnabled) }
    var transmissionFps by remember { mutableStateOf(120) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurface)
                        .clickable(onClick = onBack)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("← BACK", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "CONTROLLER CALIBRATION & HARDWARE",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Column: Steering Physics Calibration
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "STEERING WHEEL CALIBRATION",
                    color = AccentCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                // Wheel Degree options: 180°, 360°, 900°
                Text("Max Rotation Range: ${maxWheelDeg.toInt()}°", color = TextPrimary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(180f, 360f, 900f).forEach { deg ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (maxWheelDeg == deg) AccentAmber else Color(0xFF1B2335))
                                .clickable { maxWheelDeg = deg }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${deg.toInt()}°",
                                color = if (maxWheelDeg == deg) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Sensitivity Slider
                Text("Sensitivity Factor: ${String.format("%.2f", sensitivity)}x", color = TextPrimary, fontSize = 12.sp)
                Slider(
                    value = sensitivity,
                    onValueChange = { sensitivity = it },
                    valueRange = 0.5f..2.5f
                )

                // Deadzone Slider
                Text("Center Deadzone: ${(deadzone * 100).toInt()}%", color = TextPrimary, fontSize = 12.sp)
                Slider(
                    value = deadzone,
                    onValueChange = { deadzone = it },
                    valueRange = 0f..0.15f
                )

                // Auto-center spring switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Elastic Auto-Centering Spring", color = TextPrimary, fontSize = 12.sp)
                    Switch(
                        checked = autoCenter,
                        onCheckedChange = { autoCenter = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentAmber)
                    )
                }
            }

            // Right Column: Performance, Haptics & Feedback
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "TRANSMISSION & HAPTICS",
                    color = AccentCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                // Vibration Haptics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vibration Feedback & Click Haptics", color = TextPrimary, fontSize = 12.sp)
                    Switch(
                        checked = hapticsEnabled,
                        onCheckedChange = {
                            hapticsEnabled = it
                            haptics.isEnabled = it
                            if (it) haptics.playHeavyClick()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentAmber)
                    )
                }

                // Packet Transmission Rate
                Text("Packet Update Rate: $transmissionFps Hz", color = TextPrimary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(60, 90, 120).forEach { fps ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (transmissionFps == fps) AccentAmber else Color(0xFF1B2335))
                                .clickable { transmissionFps = fps }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$fps FPS",
                                color = if (transmissionFps == fps) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
                ) {
                    Text("APPLY & SAVE SETTINGS", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
