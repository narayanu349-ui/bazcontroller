package com.baz.controller.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baz.controller.ui.theme.AccentAmber
import com.baz.controller.ui.theme.DarkBorder
import com.baz.controller.ui.theme.DarkSurface
import com.baz.controller.ui.theme.TextPrimary

@Composable
fun GearSelector(
    currentGear: Int, // -1=R, 0=N, 1..6
    modifier: Modifier = Modifier,
    onGearSelected: (Int) -> Unit,
    onShiftHaptic: () -> Unit = {}
) {
    var isSequentialMode by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mode Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSequentialMode) "SEQUENTIAL" else "H-SHIFTER",
                color = AccentAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1E2638))
                    .clickable { isSequentialMode = !isSequentialMode }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isSequentialMode) "MODE: SEQ" else "MODE: H",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (isSequentialMode) {
            // Sequential Shift Buttons (+ and -)
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1B2335))
                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            if (currentGear > -1) {
                                onGearSelected(currentGear - 1)
                                onShiftHaptic()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("SHIFT -", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF0F1522)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (currentGear) {
                            -1 -> "R"
                            0 -> "N"
                            else -> "$currentGear"
                        },
                        color = AccentAmber,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1B2335))
                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            if (currentGear < 6) {
                                onGearSelected(currentGear + 1)
                                onShiftHaptic()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("SHIFT +", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        } else {
            // H-Pattern Grid
            val topRowGears = listOf(-1, 1, 3, 5) // R, 1, 3, 5
            val bottomRowGears = listOf(0, 2, 4, 6) // N, 2, 4, 6

            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Top Row (R, 1, 3, 5)
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    topRowGears.forEach { gear ->
                        GearGateButton(
                            label = if (gear == -1) "R" else "$gear",
                            isSelected = currentGear == gear,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            onGearSelected(gear)
                            onShiftHaptic()
                        }
                    }
                }

                // Bottom Row (N, 2, 4, 6)
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    bottomRowGears.forEach { gear ->
                        GearGateButton(
                            label = if (gear == 0) "N" else "$gear",
                            isSelected = currentGear == gear,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            onGearSelected(gear)
                            onShiftHaptic()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GearGateButton(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (isSelected) AccentAmber else Color(0xFF1B2335)
    val textCol = if (isSelected) Color.Black else TextPrimary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, if (isSelected) AccentAmber else DarkBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textCol,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
