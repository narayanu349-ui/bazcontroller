package com.baz.controller.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.baz.controller.ui.theme.DarkBorder
import kotlinx.coroutines.launch

enum class PedalType(val title: String, val activeColor: Color) {
    ACCELERATOR("ACCEL", Color(0xFF10B981)),
    BRAKE("BRAKE", Color(0xFFEF4444)),
    CLUTCH("CLUTCH", Color(0xFF06B6D4)),
    HANDBRAKE("EBRAKE", Color(0xFFF59E0B))
}

@Composable
fun PedalControl(
    type: PedalType,
    modifier: Modifier = Modifier,
    onValueChanged: (Float) -> Unit,
    onPedalEngaged: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val pedalAnim = remember { Animatable(0f) }
    var rawValue by remember { mutableStateOf(0f) }

    fun updateValue(v: Float) {
        val clamped = v.coerceIn(0f, 1f)
        rawValue = clamped
        onValueChanged(clamped)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        // Calculate pedal pressure based on touch height (bottom = max, top = min)
                        val pressure = 1f - (offset.y / size.height)
                        updateValue(pressure)
                        onPedalEngaged()
                        tryAwaitRelease()
                        // Spring back to 0 on release
                        coroutineScope.launch {
                            pedalAnim.snapTo(rawValue)
                            pedalAnim.animateTo(
                                0f,
                                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                            ) {
                                updateValue(this.value)
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            pedalAnim.snapTo(rawValue)
                            pedalAnim.animateTo(0f) {
                                updateValue(this.value)
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        val pressure = 1f - (change.position.y / size.height)
                        updateValue(pressure)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Background trough
            drawRoundRect(
                color = Color(0xFF101522),
                size = Size(w, h),
                cornerRadius = CornerRadius(16f, 16f)
            )

            // Outline
            drawRoundRect(
                color = DarkBorder,
                size = Size(w, h),
                cornerRadius = CornerRadius(16f, 16f),
                style = Stroke(width = 2f)
            )

            // Active pressure fill (fills from bottom upwards)
            val fillHeight = h * rawValue
            val fillTop = h - fillHeight

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(type.activeColor, type.activeColor.copy(alpha = 0.5f)),
                    startY = fillTop,
                    endY = h
                ),
                topLeft = Offset(4f, fillTop),
                size = Size(w - 8f, fillHeight - 4f),
                cornerRadius = CornerRadius(12f, 12f)
            )

            // Grip ridges
            val numRidges = 8
            for (i in 1..numRidges) {
                val yPos = (h / (numRidges + 1)) * i
                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(w * 0.18f, yPos),
                    end = Offset(w * 0.82f, yPos),
                    strokeWidth = 3f
                )
            }
        }
    }
}
