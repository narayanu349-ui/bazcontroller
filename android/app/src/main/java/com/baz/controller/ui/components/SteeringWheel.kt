package com.baz.controller.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import com.baz.controller.ui.theme.AccentAmber
import com.baz.controller.ui.theme.DarkBorder
import com.baz.controller.ui.theme.DarkSurface
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SteeringWheel(
    modifier: Modifier = Modifier,
    maxRotationDeg: Float = 360f,
    sensitivity: Float = 1.0f,
    deadzonePercent: Float = 0.02f,
    autoCenter: Boolean = true,
    onAngleChanged: (angleDeg: Float, normalized: Float) -> Unit,
    onWheelDetent: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val rotationAnim = remember { Animatable(0f) }
    var currentAngle by remember { mutableStateOf(0f) }
    var lastDetentAngle by remember { mutableStateOf(0f) }

    fun updateAngle(rawAngle: Float) {
        val clamped = rawAngle.coerceIn(-maxRotationDeg, maxRotationDeg)
        currentAngle = clamped

        // Check detent tick every 45 degrees
        if (kotlin.math.abs(clamped - lastDetentAngle) >= 45f) {
            lastDetentAngle = clamped
            onWheelDetent()
        }

        // Apply deadzone and sensitivity
        val rawNormalized = clamped / maxRotationDeg
        val absNorm = kotlin.math.abs(rawNormalized)
        val normalizedWithDeadzone = if (absNorm <= deadzonePercent) {
            0f
        } else {
            val sign = if (rawNormalized >= 0) 1f else -1f
            val remapped = (absNorm - deadzonePercent) / (1f - deadzonePercent)
            sign * (remapped * sensitivity).coerceIn(-1f, 1f)
        }

        onAngleChanged(clamped, normalizedWithDeadzone)
    }

    Box(
        modifier = modifier.pointerInput(Unit) {
            var startTouchAngle = 0f
            var startWheelAngle = 0f

            detectDragGestures(
                onDragStart = { offset ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = offset.x - center.x
                    val dy = offset.y - center.y
                    startTouchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    startWheelAngle = currentAngle
                },
                onDragEnd = {
                    if (autoCenter) {
                        coroutineScope.launch {
                            rotationAnim.snapTo(currentAngle)
                            rotationAnim.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) {
                                updateAngle(this.value)
                            }
                        }
                    }
                },
                onDrag = { change, _ ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = change.position.x - center.x
                    val dy = change.position.y - center.y
                    val touchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

                    var delta = touchAngle - startTouchAngle
                    while (delta > 180f) delta -= 360f
                    while (delta < -180f) delta += 360f

                    val newAngle = startWheelAngle + delta
                    updateAngle(newAngle)
                }
            )
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) * 0.92f
            val rimStroke = radius * 0.16f

            rotate(degrees = currentAngle, pivot = center) {
                // Outer Rim base
                drawCircle(
                    color = Color(0xFF1E2638),
                    radius = radius,
                    center = center,
                    style = Stroke(width = rimStroke)
                )

                // Grip texture marks
                for (i in 0 until 12) {
                    val angleRad = Math.toRadians((i * 30.0))
                    val gripOffset = Offset(
                        center.x + (radius * cos(angleRad)).toFloat(),
                        center.y + (radius * sin(angleRad)).toFloat()
                    )
                    drawCircle(
                        color = Color(0xFF111827),
                        radius = rimStroke * 0.35f,
                        center = gripOffset
                    )
                }

                // Inner rim border
                drawCircle(
                    color = DarkBorder,
                    radius = radius - (rimStroke / 2f),
                    center = center,
                    style = Stroke(width = 2f)
                )

                // Top 12 o'clock racing stripe
                val stripeAngleRad = Math.toRadians(-90.0)
                val stripeStart = Offset(
                    center.x + ((radius - rimStroke / 2f) * cos(stripeAngleRad)).toFloat(),
                    center.y + ((radius - rimStroke / 2f) * sin(stripeAngleRad)).toFloat()
                )
                val stripeEnd = Offset(
                    center.x + ((radius + rimStroke / 2f) * cos(stripeAngleRad)).toFloat(),
                    center.y + ((radius + rimStroke / 2f) * sin(stripeAngleRad)).toFloat()
                )
                drawLine(
                    color = AccentAmber,
                    start = stripeStart,
                    end = stripeEnd,
                    strokeWidth = 14f,
                    cap = StrokeCap.Round
                )

                // 3 Spokes (Left, Right, Bottom)
                val spokeWidth = radius * 0.22f
                val hubRadius = radius * 0.32f

                // Central Hub
                drawCircle(
                    color = DarkSurface,
                    radius = hubRadius,
                    center = center
                )
                drawCircle(
                    color = DarkBorder,
                    radius = hubRadius,
                    center = center,
                    style = Stroke(width = 3f)
                )

                // Spokes paths
                // Left Spoke (180 deg)
                drawLine(
                    color = Color(0xFF1E2638),
                    start = Offset(center.x - hubRadius, center.y),
                    end = Offset(center.x - radius + (rimStroke / 2f), center.y),
                    strokeWidth = spokeWidth,
                    cap = StrokeCap.Round
                )
                // Right Spoke (0 deg)
                drawLine(
                    color = Color(0xFF1E2638),
                    start = Offset(center.x + hubRadius, center.y),
                    end = Offset(center.x + radius - (rimStroke / 2f), center.y),
                    strokeWidth = spokeWidth,
                    cap = StrokeCap.Round
                )
                // Bottom Spoke (90 deg)
                drawLine(
                    color = Color(0xFF1E2638),
                    start = Offset(center.x, center.y + hubRadius),
                    end = Offset(center.x, center.y + radius - (rimStroke / 2f)),
                    strokeWidth = spokeWidth,
                    cap = StrokeCap.Round
                )

                // Center horn button emblem
                drawCircle(
                    color = Color(0xFF161E2E),
                    radius = hubRadius * 0.70f,
                    center = center
                )
                drawCircle(
                    color = AccentAmber.copy(alpha = 0.8f),
                    radius = hubRadius * 0.20f,
                    center = center
                )
            }
        }
    }
}
