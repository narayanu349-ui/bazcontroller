package com.baz.controller.model

import java.util.UUID

enum class ControlType {
    STEERING_WHEEL,
    ACCELERATOR_PEDAL,
    BRAKE_PEDAL,
    CLUTCH_PEDAL,
    HANDBRAKE,
    GEAR_SELECTOR,
    ANALOG_JOYSTICK,
    DPAD,
    INDICATOR_PANEL,
    LIGHTS_PANEL,
    HORN_BUTTON,
    WIPERS_PANEL,
    CRUISE_CONTROL,
    CAMERA_CLUSTER,
    TOGGLE_SWITCHES,
    CUSTOM_BUTTON_GROUP,
    DASHBOARD_CLUSTER
}

data class LayoutElement(
    val id: String = UUID.randomUUID().toString(),
    val type: ControlType,
    var x: Float, // Normalized X position (0.0 to 1.0)
    var y: Float, // Normalized Y position (0.0 to 1.0)
    var width: Float, // Normalized width
    var height: Float, // Normalized height
    var rotationDeg: Float = 0f, // Rotation in degrees (0..360)
    var isVisible: Boolean = true,
    var customLabel: String = ""
)

data class TemplateLayout(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val elements: List<LayoutElement>
)
