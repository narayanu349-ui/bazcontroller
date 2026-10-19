package com.baz.controller.connection

import android.content.Context
import com.baz.controller.protocol.BazPacket
import com.baz.controller.protocol.SecurityManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ConnectionManager(
    private val context: Context,
    private val securityManager: SecurityManager
) {
    val wifiManager = WifiTcpManager(context)
    val bluetoothClassicManager = BluetoothClassicManager(context)
    val bluetoothLeManager = BluetoothLeManager(context)
    val usbManager = UsbConnectionManager(context)

    private val _currentMode = MutableStateFlow(ConnectionMode.WIFI_TCP)
    val currentMode: StateFlow<ConnectionMode> = _currentMode

    private val _latencyMs = MutableStateFlow(3.2f)
    val latencyMs: StateFlow<Float> = _latencyMs

    private val _packetRateFps = MutableStateFlow(120)
    val packetRateFps: StateFlow<Int> = _packetRateFps

    private var sequenceCounter = 0L

    val activeStatus: Flow<ConnectionStatus> = _currentMode.flatMapLatest { mode ->
        when (mode) {
            ConnectionMode.USB -> usbManager.status
            ConnectionMode.BLUETOOTH_CLASSIC -> bluetoothClassicManager.status
            ConnectionMode.BLUETOOTH_LE -> bluetoothLeManager.status
            ConnectionMode.WIFI_TCP -> wifiManager.status
        }
    }

    fun selectMode(mode: ConnectionMode) {
        if (_currentMode.value != mode) {
            disconnectCurrent()
            _currentMode.value = mode
        }
    }

    fun disconnectCurrent() {
        when (_currentMode.value) {
            ConnectionMode.USB -> usbManager.disconnect()
            ConnectionMode.BLUETOOTH_CLASSIC -> bluetoothClassicManager.disconnect()
            ConnectionMode.BLUETOOTH_LE -> bluetoothLeManager.disconnect()
            ConnectionMode.WIFI_TCP -> wifiManager.disconnect()
        }
    }

    fun dispatchInputPacket(
        steering: Short,
        throttle: Int,
        brake: Int,
        clutch: Int,
        handbrake: Int,
        joyX: Short,
        joyY: Short,
        buttonsMask: Long,
        gear: Byte,
        wiperMode: Byte,
        lightMode: Byte,
        auxFlags: Byte
    ): Boolean {
        sequenceCounter++
        val packet = BazPacket(
            packetType = BazPacket.TYPE_INPUT_FRAME,
            sequenceNumber = sequenceCounter,
            timestampUs = System.nanoTime() / 1000L,
            steering = steering,
            throttle = throttle,
            brake = brake,
            clutch = clutch,
            handbrake = handbrake,
            joyX = joyX,
            joyY = joyY,
            buttonsMask = buttonsMask,
            gear = gear,
            wiperMode = wiperMode,
            lightMode = lightMode,
            auxiliaryFlags = auxFlags
        )

        return when (_currentMode.value) {
            ConnectionMode.USB -> usbManager.sendPacket(packet)
            ConnectionMode.BLUETOOTH_CLASSIC -> bluetoothClassicManager.sendPacket(packet)
            ConnectionMode.BLUETOOTH_LE -> bluetoothLeManager.sendPacket(packet)
            ConnectionMode.WIFI_TCP -> wifiManager.sendPacket(packet)
        }
    }
}
