package com.baz.controller.connection

import android.content.Context
import android.net.wifi.WifiManager
import com.baz.controller.protocol.BazPacket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class WifiTcpManager(private val context: Context) {

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status: StateFlow<ConnectionStatus> = _status

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices

    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    private var ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false
    private var targetHost: String = "192.168.1.100"
    private var targetPort: Int = 8990
    private var autoReconnectEnabled = true

    fun startDiscovery() {
        ioScope.launch {
            try {
                val udpSocket = DatagramSocket(8991)
                udpSocket.soTimeout = 4000
                val buffer = ByteArray(256)
                val packet = DatagramPacket(buffer, buffer.size)

                val devices = mutableListOf<DiscoveredDevice>()
                val startTime = System.currentTimeMillis()

                while (System.currentTimeMillis() - startTime < 6000 && isActive) {
                    try {
                        udpSocket.receive(packet)
                        val msg = String(packet.data, 0, packet.length)
                        if (msg.startsWith("BAZ_COMPANION_BEACON")) {
                            val parts = msg.split(";")
                            val serverName = parts.getOrNull(1) ?: "Windows Gaming PC"
                            val port = parts.getOrNull(2)?.toIntOrNull() ?: 8990
                            val dev = DiscoveredDevice(
                                id = packet.address.hostAddress ?: "wifi-host",
                                name = serverName,
                                address = "${packet.address.hostAddress}:$port",
                                mode = ConnectionMode.WIFI_TCP
                            )
                            if (devices.none { it.id == dev.id }) {
                                devices.add(dev)
                                _discoveredDevices.value = devices.toList()
                            }
                        }
                    } catch (e: Exception) {
                        // Timeout tick
                    }
                }
                udpSocket.close()
            } catch (e: Exception) {
                // Ignore UDP broadcast bind issues
            }
        }
    }

    fun connect(host: String, port: Int = 8990) {
        targetHost = host
        targetPort = port
        isRunning = true
        _status.value = ConnectionStatus.CONNECTING

        ioScope.launch {
            runConnectionLoop()
        }
    }

    private suspend fun runConnectionLoop() {
        while (isRunning) {
            try {
                _status.value = ConnectionStatus.CONNECTING
                val newSocket = Socket()
                newSocket.tcpNoDelay = true
                newSocket.keepAlive = true
                newSocket.sendBufferSize = 64 * 1024
                newSocket.connect(InetSocketAddress(targetHost, targetPort), 3000)

                socket = newSocket
                outputStream = newSocket.getOutputStream()
                inputStream = newSocket.getInputStream()

                _status.value = ConnectionStatus.CONNECTED

                // Read feedback packets / ping from companion server
                val rxBuffer = ByteArray(BazPacket.PACKET_SIZE)
                while (isRunning && newSocket.isConnected && !newSocket.isClosed) {
                    val read = inputStream?.read(rxBuffer) ?: -1
                    if (read == -1) break
                }
            } catch (e: Exception) {
                _status.value = if (autoReconnectEnabled && isRunning) {
                    ConnectionStatus.RECONNECTING
                } else {
                    ConnectionStatus.DISCONNECTED
                }
            } finally {
                closeSocket()
            }

            if (autoReconnectEnabled && isRunning) {
                delay(1500)
            } else {
                break
            }
        }
        _status.value = ConnectionStatus.DISCONNECTED
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
        closeSocket()
        _status.value = ConnectionStatus.DISCONNECTED
    }

    private fun closeSocket() {
        try {
            outputStream?.close()
            inputStream?.close()
            socket?.close()
        } catch (_: Exception) {}
        outputStream = null
        inputStream = null
        socket = null
    }
}
