package com.baz.controller.connection

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.baz.controller.protocol.BazPacket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothClassicManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        manager?.adapter
    }

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status: StateFlow<ConnectionStatus> = _status

    private val _pairedDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val pairedDevices: StateFlow<List<DiscoveredDevice>> = _pairedDevices

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        val adapter = bluetoothAdapter ?: return
        try {
            val bonded = adapter.bondedDevices
            val list = bonded.map { dev ->
                DiscoveredDevice(
                    id = dev.address,
                    name = dev.name ?: "Unknown Device",
                    address = dev.address,
                    mode = ConnectionMode.BLUETOOTH_CLASSIC,
                    isPaired = true
                )
            }
            _pairedDevices.value = list
        } catch (e: Exception) {
            _pairedDevices.value = emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(macAddress: String) {
        val adapter = bluetoothAdapter ?: return
        isRunning = true
        _status.value = ConnectionStatus.CONNECTING

        scope.launch {
            try {
                val device: BluetoothDevice = adapter.getRemoteDevice(macAddress)
                adapter.cancelDiscovery()

                val s = device.createRfcommSocketToServiceRecord(sppUuid)
                s.connect()

                socket = s
                outputStream = s.outputStream
                inputStream = s.inputStream
                _status.value = ConnectionStatus.CONNECTED

                val buffer = ByteArray(BazPacket.PACKET_SIZE)
                while (isRunning && s.isConnected) {
                    val read = inputStream?.read(buffer) ?: -1
                    if (read == -1) break
                }
            } catch (e: Exception) {
                _status.value = ConnectionStatus.ERROR
            } finally {
                disconnect()
            }
        }
    }

    fun sendPacket(packet: BazPacket): Boolean {
        if (_status.value != ConnectionStatus.CONNECTED) return false
        val bytes = BazPacket.serialize(packet)
        return try {
            outputStream?.write(bytes)
            outputStream?.flush()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun disconnect() {
        isRunning = false
        try {
            outputStream?.close()
            inputStream?.close()
            socket?.close()
        } catch (_: Exception) {}
        outputStream = null
        inputStream = null
        socket = null
        _status.value = ConnectionStatus.DISCONNECTED
    }
}
