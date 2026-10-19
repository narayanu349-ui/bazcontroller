package com.baz.controller.connection

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import com.baz.controller.protocol.BazPacket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class BluetoothLeManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val serviceUuid: UUID = UUID.fromString("6BA7B810-9DAD-11D1-80B4-00C04FD430C8")
    private val charTxUuid: UUID = UUID.fromString("6BA7B811-9DAD-11D1-80B4-00C04FD430C8")

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status: StateFlow<ConnectionStatus> = _status

    private val _scannedDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val scannedDevices: StateFlow<List<DiscoveredDevice>> = _scannedDevices

    private var gatt: BluetoothGatt? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null

    @SuppressLint("MissingPermission")
    fun startScan() {
        val scanner = adapter?.bluetoothLeScanner ?: return
        _status.value = ConnectionStatus.DISCOVERING
        val map = mutableMapOf<String, DiscoveredDevice>()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val dev = result.device
                val name = dev.name ?: "BLE Companion PC"
                val deviceObj = DiscoveredDevice(
                    id = dev.address,
                    name = name,
                    address = dev.address,
                    mode = ConnectionMode.BLUETOOTH_LE,
                    rssi = result.rssi
                )
                map[dev.address] = deviceObj
                _scannedDevices.value = map.values.toList()
            }
        }
        scanner.startScan(callback)
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String) {
        val dev = adapter?.getRemoteDevice(address) ?: return
        _status.value = ConnectionStatus.CONNECTING

        gatt = dev.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    _status.value = ConnectionStatus.CONNECTED
                    g.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                    g.requestMtu(512)
                    g.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    _status.value = ConnectionStatus.DISCONNECTED
                    txCharacteristic = null
                }
            }

            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val s = g.getService(serviceUuid)
                    txCharacteristic = s?.getCharacteristic(charTxUuid)
                }
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun sendPacket(packet: BazPacket): Boolean {
        val g = gatt ?: return false
        val c = txCharacteristic ?: return false
        val bytes = BazPacket.serialize(packet)

        c.value = bytes
        c.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        return g.writeCharacteristic(c)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        txCharacteristic = null
        _status.value = ConnectionStatus.DISCONNECTED
    }
}
