package com.baz.controller.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.baz.controller.ui.theme.AccentAmber
import com.baz.controller.ui.theme.DarkBorder
import com.baz.controller.ui.theme.DarkSurface
import kotlin.math.min
import kotlin.math.sqrt

@Composable
fun AnalogStick(
    modifier: Modifier = Modifier,
    onStickMoved: (x: Float, y: Float) -> Unit
) {
    var stickOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier.pointerInput(Unit) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = min(size.width, size.height) / 2f * 0.7f

            detectDragGestures(
                onDragStart = { offset ->
                    val delta = offset - center
                    val dist = sqrt(delta.x * delta.x + delta.y * delta.y)
                    val clampedDist = min(dist, maxRadius)
                    val factor = if (dist > 0) clampedDist / dist else 0f
                    val clamped = Offset(delta.x * factor, delta.y * factor)
                    stickOffset = clamped
                    onStickMoved(clamped.x / maxRadius, clamped.y / maxRadius)
                },
                onDragEnd = {
                    stickOffset = Offset.Zero
                    onStickMoved(0f, 0f)
                },
                onDrag = { change, _ ->
                    val delta = change.position - center
                    val dist = sqrt(delta.x * delta.x + delta.y * delta.y)
                    val clampedDist = min(dist, maxRadius)
                    val factor = if (dist > 0) clampedDist / dist else 0f
                    val clamped = Offset(delta.x * factor, delta.y * factor)
                    stickOffset = clamped
                    onStickMoved(clamped.x / maxRadius, clamped.y / maxRadius)
                }
            )
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = min(size.width, size.height) / 2f * 0.85f
            val thumbRadius = outerRadius * 0.38f

            // Outer ring base
            drawCircle(
                color = DarkSurface,
                radius = outerRadius,
                center = center
            )
            drawCircle(
                color = DarkBorder,
                radius = outerRadius,
                center = center,
                style = Stroke(width = 2f)
            )

            // Inner cross guide
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(center.x - outerRadius * 0.6f, center.y),
                end = Offset(center.x + outerRadius * 0.6f, center.y),
                strokeWidth = 1.5f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(center.x, center.y - outerRadius * 0.6f),
                end = Offset(center.x, center.y + outerRadius * 0.6f),
                strokeWidth = 1.5f
            )

            // Draggable Thumb Knob
            val thumbCenter = center + stickOffset
            drawCircle(
                color = Color(0xFF1E2638),
                radius = thumbRadius,
                center = thumbCenter
            )
            drawCircle(
                color = AccentAmber,
                radius = thumbRadius,
                center = thumbCenter,
                style = Stroke(width = 2.5f)
            )
            drawCircle(
                color = AccentAmber.copy(alpha = 0.6f),
                radius = thumbRadius * 0.35f,
                center = thumbCenter
            )
        }
    }
}
