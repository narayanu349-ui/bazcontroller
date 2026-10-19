package com.baz.controller.connection

enum class ConnectionMode(val displayName: String) {
    USB("USB Direct"),
    BLUETOOTH_CLASSIC("Bluetooth Classic (SPP)"),
    BLUETOOTH_LE("Bluetooth Low Energy (BLE)"),
    WIFI_TCP("Wi-Fi TCP Socket")
}

enum class ConnectionStatus {
    DISCONNECTED,
    DISCOVERING,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

data class DiscoveredDevice(
    val id: String,
    val name: String,
    val address: String,
    val mode: ConnectionMode,
    val rssi: Int = 0,
    val isPaired: Boolean = false
)
