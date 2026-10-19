package com.baz.controller.model

data class ControllerState(
    val steeringAngleDeg: Float = 0f, // -180f to +180f or 360 degree mode
    val normalizedSteering: Float = 0f, // -1.0 to 1.0
    val throttle: Float = 0f, // 0.0 to 1.0
    val brake: Float = 0f, // 0.0 to 1.0
    val clutch: Float = 0f, // 0.0 to 1.0
    val handbrake: Float = 0f, // 0.0 to 1.0
    val joyX: Float = 0f, // -1.0 to 1.0
    val joyY: Float = 0f, // -1.0 to 1.0
    val gear: Int = 0, // -1: R, 0: N, 1..6
    val indicatorLeft: Boolean = false,
    val indicatorRight: Boolean = false,
    val hazards: Boolean = false,
    val headlightMode: Int = 0, // 0: Off, 1: Low, 2: High
    val horn: Boolean = false,
    val wiperMode: Int = 0, // 0: Off, 1: Int, 2: Norm, 3: Fast
    val cruiseControlActive: Boolean = false,
    val cruiseTargetSpeed: Int = 80,
    val diffLock: Boolean = false,
    val engineStarted: Boolean = true,
    val beacon: Boolean = false,
    val retarderLevel: Int = 0,
    val activeCamera: Int = 1, // 1: Cockpit, 2: Chase, 3: Free, 4: Top
    val customButtonsState: Map<Int, Boolean> = emptyMap(),
    val dpadUp: Boolean = false,
    val dpadDown: Boolean = false,
    val dpadLeft: Boolean = false,
    val dpadRight: Boolean = false
)
