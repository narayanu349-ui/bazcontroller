package com.baz.controller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baz.controller.BazApplication
import com.baz.controller.connection.ConnectionStatus
import com.baz.controller.model.ControllerState
import com.baz.controller.protocol.BazPacket
import com.baz.controller.ui.components.*
import com.baz.controller.ui.theme.AccentAmber
import com.baz.controller.ui.theme.AccentCyan
import com.baz.controller.ui.theme.DarkBg
import com.baz.controller.ui.theme.DarkBorder
import com.baz.controller.ui.theme.DarkSurface
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun ControllerScreen(
    onNavigateToConnection: () -> Unit,
    onNavigateToEditor: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTemplates: () -> Unit
) {
    val app = BazApplication.instance
    val connManager = app.connectionManager
    val haptics = app.hapticsManager
    val templateRepo = app.templateRepository

    val activeTemplate by templateRepo.activeTemplate.collectAsState()
    val connStatus by connManager.activeStatus.collectAsState(initial = ConnectionStatus.DISCONNECTED)
    val latencyMs by connManager.latencyMs.collectAsState()

    var state by remember { mutableStateOf(ControllerState()) }

    // High performance transmission loop (120 Hz)
    LaunchedEffect(connStatus) {
        var seq = 0L
        while (true) {
            seq++
            var buttonsMask = 0L

            if (state.dpadUp) buttonsMask = buttonsMask or BazPacket.BTN_DPAD_UP
            if (state.dpadDown) buttonsMask = buttonsMask or BazPacket.BTN_DPAD_DOWN
            if (state.dpadLeft) buttonsMask = buttonsMask or BazPacket.BTN_DPAD_LEFT
            if (state.dpadRight) buttonsMask = buttonsMask or BazPacket.BTN_DPAD_RIGHT
            if (state.indicatorLeft) buttonsMask = buttonsMask or BazPacket.BTN_INDICATOR_LEFT
            if (state.indicatorRight) buttonsMask = buttonsMask or BazPacket.BTN_INDICATOR_RIGHT
            if (state.hazards) buttonsMask = buttonsMask or BazPacket.BTN_HAZARDS
            if (state.headlightMode > 0) buttonsMask = buttonsMask or BazPacket.BTN_HEADLIGHTS
            if (state.headlightMode == 2) buttonsMask = buttonsMask or BazPacket.BTN_HIGH_BEAM
            if (state.horn) buttonsMask = buttonsMask or BazPacket.BTN_HORN
            if (state.wiperMode > 0) buttonsMask = buttonsMask or BazPacket.BTN_WIPERS
            if (state.cruiseControlActive) buttonsMask = buttonsMask or BazPacket.BTN_CRUISE_SET
            if (state.diffLock) buttonsMask = buttonsMask or BazPacket.BTN_DIFF_LOCK
            if (state.beacon) buttonsMask = buttonsMask or BazPacket.BTN_BEACON

            when (state.gear) {
                -1 -> buttonsMask = buttonsMask or BazPacket.BTN_GEAR_R
                1 -> buttonsMask = buttonsMask or BazPacket.BTN_GEAR_1
                2 -> buttonsMask = buttonsMask or BazPacket.BTN_GEAR_2
                3 -> buttonsMask = buttonsMask or BazPacket.BTN_GEAR_3
                4 -> buttonsMask = buttonsMask or BazPacket.BTN_GEAR_4
                5 -> buttonsMask = buttonsMask or BazPacket.BTN_GEAR_5
                6 -> buttonsMask = buttonsMask or BazPacket.BTN_GEAR_6
            }

            connManager.dispatchInputPacket(
                steering = (state.normalizedSteering * 32767).toInt().toShort(),
                throttle = (state.throttle * 65535).toInt(),
                brake = (state.brake * 65535).toInt(),
                clutch = (state.clutch * 65535).toInt(),
                handbrake = (state.handbrake * 65535).toInt(),
                joyX = (state.joyX * 32767).toInt().toShort(),
                joyY = (state.joyY * 32767).toInt().toShort(),
                buttonsMask = buttonsMask,
                gear = state.gear.toByte(),
                wiperMode = state.wiperMode.toByte(),
                lightMode = state.headlightMode.toByte(),
                auxFlags = if (state.cruiseControlActive) 1.toByte() else 0.toByte()
            )

            delay(8) // ~120 FPS
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF11141B))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Ultra-Compact Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "BAZCONTROLLER",
                        color = AccentAmber,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "· ${activeTemplate.name}",
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurface)
                            .clickable(onClick = onNavigateToConnection)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "UDP <8ms · ${connStatus.name}",
                            color = if (connStatus == ConnectionStatus.CONNECTED) Color(0xFF10B981) else Color(0xFFEF4444),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurface)
                            .clickable(onClick = onNavigateToTemplates)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("TEMPLATES", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurface)
                            .clickable(onClick = onNavigateToEditor)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("STUDIO", color = AccentCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurface)
                            .clickable(onClick = onNavigateToSettings)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("SETTINGS", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Edge-to-Edge Cockpit Row (Fill 100% of height)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Zone 1: Left Thumb - 360° Steering Wheel & Paddle Shifters
                Box(
                    modifier = Modifier
                        .weight(0.36f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    // Left Paddle Shifter (-)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(28.dp)
                            .height(110.dp)
                            .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                            .background(Color(0xFF1D222F))
                            .border(1.dp, Color(0xFF333B50), RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                            .clickable {
                                haptics.playClick()
                                if (state.gear > -1) state = state.copy(gear = state.gear - 1)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("−", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }

                    // Steering Wheel
                    SteeringWheel(
                        modifier = Modifier.fillMaxSize(0.92f),
                        maxRotationDeg = 360f,
                        sensitivity = 1.0f,
                        onAngleChanged = { angle, norm ->
                            state = state.copy(steeringAngleDeg = angle, normalizedSteering = norm)
                        },
                        onWheelDetent = { haptics.playWheelDetent() }
                    )

                    // Right Paddle Shifter (+)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(28.dp)
                            .height(110.dp)
                            .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                            .background(Color(0xFF1D222F))
                            .border(1.dp, Color(0xFF333B50), RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                            .clickable {
                                haptics.playClick()
                                if (state.gear < 6) state = state.copy(gear = state.gear + 1)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                }

                // Zone 2: Center Console - Rockers, Red Engine Start & Amber LCD
                Column(
                    modifier = Modifier
                        .weight(0.34f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Rockers Grid (6 buttons)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        // Headlights
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (state.headlightMode > 0) Color(0xFF1E283A) else Color(0xFF171A23))
                                .border(1.dp, if (state.headlightMode > 0) AccentCyan else DarkBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    haptics.playClick()
                                    val next = (state.headlightMode + 1) % 3
                                    state = state.copy(headlightMode = next)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("LIGHTS", color = if (state.headlightMode > 0) AccentCyan else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        // High beam
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (state.headlightMode == 2) Color(0xFF1E283A) else Color(0xFF171A23))
                                .border(1.dp, if (state.headlightMode == 2) AccentCyan else DarkBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    haptics.playClick()
                                    val next = if (state.headlightMode == 2) 1 else 2
                                    state = state.copy(headlightMode = next)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("BEAM", color = if (state.headlightMode == 2) AccentCyan else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        // Beacon
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (state.beacon) Color(0xFF2C2215) else Color(0xFF171A23))
                                .border(1.dp, if (state.beacon) AccentAmber else DarkBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    haptics.playClick()
                                    state = state.copy(beacon = !state.beacon)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("BEACON", color = if (state.beacon) AccentAmber else Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }

                        // Wipers
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (state.wiperMode > 0) Color(0xFF2C2215) else Color(0xFF171A23))
                                .border(1.dp, if (state.wiperMode > 0) AccentAmber else DarkBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    haptics.playClick()
                                    val next = (state.wiperMode + 1) % 4
                                    state = state.copy(wiperMode = next)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("WIPER", color = if (state.wiperMode > 0) AccentAmber else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        // Diff Lock
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (state.diffLock) Color(0xFF2D1F1A) else Color(0xFF171A23))
                                .border(1.dp, if (state.diffLock) Color(0xFFF97316) else DarkBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    haptics.playClick()
                                    state = state.copy(diffLock = !state.diffLock)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("DIFF", color = if (state.diffLock) Color(0xFFF97316) else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        // Cruise
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (state.cruiseControlActive) Color(0xFF18281E) else Color(0xFF171A23))
                                .border(1.dp, if (state.cruiseControlActive) Color(0xFF10B981) else DarkBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    haptics.playClick()
                                    state = state.copy(cruiseControlActive = !state.cruiseControlActive)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("CRUISE", color = if (state.cruiseControlActive) Color(0xFF10B981) else Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Middle Controls: Red Engine Start & Hazard Warning
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Red Engine Start Button
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (state.engineStarted) Color(0xFF991B1B) else Color(0xFF450A0A))
                                .border(3.dp, if (state.engineStarted) Color(0xFFEF4444) else Color(0xFF7F1D1D), CircleShape)
                                .clickable {
                                    haptics.playHeavyClick()
                                    state = state.copy(engineStarted = !state.engineStarted)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ENGINE\nSTART",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                lineHeight = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        // Hazard Warning Triangle
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (state.hazards) Color(0xFFDC2626) else Color(0xFF1F222C))
                                .border(2.dp, if (state.hazards) Color(0xFFF87171) else DarkBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    haptics.playClick()
                                    state = state.copy(hazards = !state.hazards)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▲", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Glowing Amber LCD Truck Radio Display (Exact match to Image 2 & 4)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E1405))
                            .border(2.dp, Color(0xFF422F10), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "[ 8 ALBI L18 ] · CH 19",
                                color = AccentAmber,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "SPD: ${(abs(state.throttle - state.brake) * 110).toInt()} KM/H",
                                    color = AccentAmber,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "GEAR: ${if (state.gear == -1) "R" else if (state.gear == 0) "N" else "D${state.gear}"}",
                                    color = AccentAmber,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "<8ms UDP",
                                    color = AccentAmber,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // Zone 3: Right Thumb - Pedals & Rotary Gear Knob
                Column(
                    modifier = Modifier
                        .weight(0.30f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top: Gear Buttons (P - R - N - D)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(-1 to "R", 0 to "N", 1 to "D", 2 to "M").forEach { (gearVal, label) ->
                            val isSelected = (gearVal == -1 && state.gear == -1) ||
                                             (gearVal == 0 && state.gear == 0) ||
                                             (gearVal == 1 && state.gear >= 1)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) AccentAmber else Color(0xFF1B1F2A))
                                    .clickable {
                                        haptics.playGearShift()
                                        state = state.copy(gear = gearVal)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Bottom: Realistic Pedals (Brake & Accelerator)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Brake Pedal
                        PedalControl(
                            type = PedalType.BRAKE,
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight(),
                            onValueChanged = { state = state.copy(brake = it) },
                            onPedalEngaged = { haptics.playClick() }
                        )

                        // Accelerator Pedal
                        PedalControl(
                            type = PedalType.ACCELERATOR,
                            modifier = Modifier
                                .weight(1.0f)
                                .fillMaxHeight(),
                            onValueChanged = { state = state.copy(throttle = it) },
                            onPedalEngaged = { haptics.playClick() }
                        )
                    }
                }
            }
        }
    }
}
