package com.baz.controller.connection

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.ParcelFileDescriptor
import com.baz.controller.protocol.BazPacket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream

class UsbConnectionManager(private val context: Context) {

    private val usbManager: UsbManager =
        context.getSystemService(Context.USB_SERVICE) as UsbManager

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status: StateFlow<ConnectionStatus> = _status

    private var accessory: UsbAccessory? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var inputStream: FileInputStream? = null
    private var outputStream: FileOutputStream? = null

    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun checkAndConnectUsb() {
        val accessoryList = usbManager.accessoryList
        if (!accessoryList.isNullOrEmpty()) {
            val target = accessoryList[0]
            if (usbManager.hasPermission(target)) {
                openAccessory(target)
            } else {
                val permissionIntent = PendingIntent.getBroadcast(
                    context, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
                )
                usbManager.requestPermission(target, permissionIntent)
            }
        }
    }

    fun openAccessory(target: UsbAccessory) {
        accessory = target
        fileDescriptor = usbManager.openAccessory(target)
        val fd: FileDescriptor? = fileDescriptor?.fileDescriptor
        if (fd != null) {
            inputStream = FileInputStream(fd)
            outputStream = FileOutputStream(fd)
            _status.value = ConnectionStatus.CONNECTED
            isRunning = true

            scope.launch {
                val buffer = ByteArray(BazPacket.PACKET_SIZE)
                while (isRunning) {
                    try {
                        val read = inputStream?.read(buffer) ?: -1
                        if (read == -1) break
                    } catch (e: Exception) {
                        break
                    }
                }
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
            inputStream?.close()
            outputStream?.close()
            fileDescriptor?.close()
        } catch (_: Exception) {}
        inputStream = null
        outputStream = null
        fileDescriptor = null
        accessory = null
        _status.value = ConnectionStatus.DISCONNECTED
    }

    companion object {
        const val ACTION_USB_PERMISSION = "com.baz.controller.USB_PERMISSION"
    }
}
