package com.baz.controller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baz.controller.BazApplication
import com.baz.controller.connection.ConnectionMode
import com.baz.controller.connection.ConnectionStatus
import com.baz.controller.ui.theme.AccentAmber
import com.baz.controller.ui.theme.AccentCyan
import com.baz.controller.ui.theme.DarkBg
import com.baz.controller.ui.theme.DarkBorder
import com.baz.controller.ui.theme.DarkSurface
import com.baz.controller.ui.theme.TextPrimary
import com.baz.controller.ui.theme.TextSecondary

@Composable
fun ConnectionScreen(onBack: () -> Unit) {
    val app = BazApplication.instance
    val connManager = app.connectionManager
    val securityManager = app.securityManager

    val currentMode by connManager.currentMode.collectAsState()
    val status by connManager.activeStatus.collectAsState(initial = ConnectionStatus.DISCONNECTED)
    val wifiDevices by connManager.wifiManager.discoveredDevices.collectAsState()
    val btDevices by connManager.bluetoothClassicManager.pairedDevices.collectAsState()
    val bleDevices by connManager.bluetoothLeManager.scannedDevices.collectAsState()

    var manualIp by remember { mutableStateOf("192.168.1.100") }
    var manualPort by remember { mutableStateOf("8990") }
    var currentPin by remember { mutableStateOf(securityManager.pairingPin) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurface)
                        .clickable(onClick = onBack)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("← BACK", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "CONNECTION & PAIRING HUB",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Status Badge
            Text(
                text = "STATUS: ${status.name}",
                color = if (status == ConnectionStatus.CONNECTED) Color(0xFF10B981) else Color(0xFFEF4444),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Transport Mode Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConnectionMode.values().forEach { mode ->
                val isSelected = currentMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) AccentAmber else DarkSurface)
                        .border(1.dp, if (isSelected) AccentAmber else DarkBorder, RoundedCornerShape(8.dp))
                        .clickable { connManager.selectMode(mode) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.displayName,
                        color = if (isSelected) Color.Black else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Transport Details Panel
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Column: Configuration & Actions
            Column(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "TRANSMISSION SETTINGS",
                    color = AccentCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                when (currentMode) {
                    ConnectionMode.WIFI_TCP -> {
                        OutlinedTextField(
                            value = manualIp,
                            onValueChange = { manualIp = it },
                            label = { Text("PC IP Address") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = manualPort,
                            onValueChange = { manualPort = it },
                            label = { Text("Port (Default 8990)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val port = manualPort.toIntOrNull() ?: 8990
                                    connManager.wifiManager.connect(manualIp, port)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
                            ) {
                                Text("CONNECT TCP", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { connManager.wifiManager.startDiscovery() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638))
                            ) {
                                Text("AUTO SCAN", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }

                    ConnectionMode.BLUETOOTH_CLASSIC -> {
                        Text(
                            text = "Connects via Bluetooth SPP (Serial Port Profile) to Windows Bluetooth receiver with <8ms packet loop.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Button(
                            onClick = { connManager.bluetoothClassicManager.refreshPairedDevices() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
                        ) {
                            Text("REFRESH PAIRED PCS", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    ConnectionMode.BLUETOOTH_LE -> {
                        Text(
                            text = "GATT Low Energy transmission with 512 MTU high-speed throughput channel.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Button(
                            onClick = { connManager.bluetoothLeManager.startScan() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
                        ) {
                            Text("SCAN FOR BLE SERVERS", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    ConnectionMode.USB -> {
                        Text(
                            text = "Ultra-low latency (<1ms) via USB Direct Android Open Accessory (AOA) or CDC serial connection.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Button(
                            onClick = { connManager.usbManager.checkAndConnectUsb() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
                        ) {
                            Text("INITIALIZE USB DIRECT", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                // Security PIN pairing card
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F1522))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SECURITY PAIRING PIN", color = Color(0xFF9CA3AF), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "RE-GEN",
                                color = AccentCyan,
                                fontSize = 10.sp,
                                modifier = Modifier.clickable {
                                    currentPin = securityManager.regeneratePin()
                                }
                            )
                        }
                        Text(
                            text = currentPin,
                            color = AccentAmber,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Enter this 6-digit code in the Windows Companion app to complete AES-128 authentication.",
                            color = Color(0xFF6B7280),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Right Column: Discovered / Paired Devices List
            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "DETECTED SERVERS & PEERS",
                    color = AccentCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(10.dp))

                val devices = when (currentMode) {
                    ConnectionMode.WIFI_TCP -> wifiDevices
                    ConnectionMode.BLUETOOTH_CLASSIC -> btDevices
                    ConnectionMode.BLUETOOTH_LE -> bleDevices
                    ConnectionMode.USB -> emptyList()
                }

                if (devices.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No companion servers detected yet.\nRun the Windows Companion app or connect via USB cable.",
                            color = Color(0xFF6B7280),
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(devices) { dev ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF192233))
                                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        when (currentMode) {
                                            ConnectionMode.WIFI_TCP -> {
                                                val host = dev.address.substringBefore(":")
                                                val port = dev.address.substringAfter(":").toIntOrNull() ?: 8990
                                                connManager.wifiManager.connect(host, port)
                                            }
                                            ConnectionMode.BLUETOOTH_CLASSIC -> {
                                                connManager.bluetoothClassicManager.connect(dev.address)
                                            }
                                            ConnectionMode.BLUETOOTH_LE -> {
                                                connManager.bluetoothLeManager.connect(dev.address)
                                            }
                                            else -> {}
                                        }
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = dev.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = dev.address, color = Color(0xFF9CA3AF), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                                Button(
                                    onClick = {
                                        when (currentMode) {
                                            ConnectionMode.WIFI_TCP -> {
                                                val host = dev.address.substringBefore(":")
                                                val port = dev.address.substringAfter(":").toIntOrNull() ?: 8990
                                                connManager.wifiManager.connect(host, port)
                                            }
                                            ConnectionMode.BLUETOOTH_CLASSIC -> {
                                                connManager.bluetoothClassicManager.connect(dev.address)
                                            }
                                            ConnectionMode.BLUETOOTH_LE -> {
                                                connManager.bluetoothLeManager.connect(dev.address)
                                            }
                                            else -> {}
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
                                ) {
                                    Text("CONNECT", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
