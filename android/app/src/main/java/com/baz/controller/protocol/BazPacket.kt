package com.baz.controller.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32

/**
 * Unified 64-Byte binary packet protocol used across USB, Bluetooth Classic,
 * Bluetooth Low Energy, and Wi-Fi TCP transports.
 */
data class BazPacket(
    val packetType: Byte = TYPE_INPUT_FRAME,
    val sequenceNumber: Long = 0L,
    val timestampUs: Long = System.nanoTime() / 1000L,
    // Axes (-32768..32767 or 0..65535)
    val steering: Short = 0,         // -32768 to 32767
    val throttle: Int = 0,           // 0 to 65535
    val brake: Int = 0,              // 0 to 65535
    val clutch: Int = 0,             // 0 to 65535
    val handbrake: Int = 0,          // 0 to 65535
    val joyX: Short = 0,             // -32768 to 32767
    val joyY: Short = 0,             // -32768 to 32767
    // Digital buttons bitmask
    val buttonsMask: Long = 0L,      // 64 discrete action buttons
    // Auxiliary vehicle states
    val gear: Byte = 0,              // -1=R, 0=N, 1..6
    val wiperMode: Byte = 0,         // 0=Off, 1=Int, 2=Norm, 3=Fast
    val lightMode: Byte = 0,         // 0=Off, 1=Low, 2=High
    val auxiliaryFlags: Byte = 0,    // Hazards, Cruise active, etc.
    // Auth / Security
    val authTag: ByteArray = ByteArray(12),
    var crc32: Long = 0L
) {
    companion object {
        const val PACKET_SIZE = 64
        const val MAGIC_0: Byte = 0xBA.toByte()
        const val MAGIC_1: Byte = 0x5C.toByte()
        const val PROTOCOL_VERSION: Byte = 0x01

        // Packet Types
        const val TYPE_INPUT_FRAME: Byte = 0x01
        const val TYPE_HANDSHAKE_REQ: Byte = 0x02
        const val TYPE_HANDSHAKE_ACK: Byte = 0x03
        const val TYPE_HEARTBEAT: Byte = 0x04
        const val TYPE_FEEDBACK_FORCE: Byte = 0x05
        const val TYPE_DISCONNECT: Byte = 0x06

        // Button Bitmask Positions
        const val BTN_DPAD_UP: Long = 1L shl 0
        const val BTN_DPAD_DOWN: Long = 1L shl 1
        const val BTN_DPAD_LEFT: Long = 1L shl 2
        const val BTN_DPAD_RIGHT: Long = 1L shl 3
        const val BTN_GEAR_R: Long = 1L shl 4
        const val BTN_GEAR_1: Long = 1L shl 5
        const val BTN_GEAR_2: Long = 1L shl 6
        const val BTN_GEAR_3: Long = 1L shl 7
        const val BTN_GEAR_4: Long = 1L shl 8
        const val BTN_GEAR_5: Long = 1L shl 9
        const val BTN_GEAR_6: Long = 1L shl 10
        const val BTN_SHIFT_UP: Long = 1L shl 11
        const val BTN_SHIFT_DOWN: Long = 1L shl 12
        const val BTN_INDICATOR_LEFT: Long = 1L shl 13
        const val BTN_INDICATOR_RIGHT: Long = 1L shl 14
        const val BTN_HAZARDS: Long = 1L shl 15
        const val BTN_HEADLIGHTS: Long = 1L shl 16
        const val BTN_HIGH_BEAM: Long = 1L shl 17
        const val BTN_HORN: Long = 1L shl 18
        const val BTN_WIPERS: Long = 1L shl 19
        const val BTN_CRUISE_SET: Long = 1L shl 20
        const val BTN_CRUISE_RES: Long = 1L shl 21
        const val BTN_CRUISE_PLUS: Long = 1L shl 22
        const val BTN_CRUISE_MINUS: Long = 1L shl 23
        const val BTN_CAM_COCKPIT: Long = 1L shl 24
        const val BTN_CAM_CHASE: Long = 1L shl 25
        const val BTN_CAM_FREE: Long = 1L shl 26
        const val BTN_CAM_TOP: Long = 1L shl 27
        const val BTN_DIFF_LOCK: Long = 1L shl 28
        const val BTN_ENGINE_START: Long = 1L shl 29
        const val BTN_BEACON: Long = 1L shl 30
        const val BTN_RETARDER: Long = 1L shl 31
        const val BTN_CUSTOM_1: Long = 1L shl 32
        const val BTN_CUSTOM_2: Long = 1L shl 33
        const val BTN_CUSTOM_3: Long = 1L shl 34
        const val BTN_CUSTOM_4: Long = 1L shl 35
        const val BTN_CUSTOM_5: Long = 1L shl 36
        const val BTN_CUSTOM_6: Long = 1L shl 37
        const val BTN_CUSTOM_7: Long = 1L shl 38
        const val BTN_CUSTOM_8: Long = 1L shl 39

        fun serialize(packet: BazPacket): ByteArray {
            val buffer = ByteBuffer.allocate(PACKET_SIZE).order(ByteOrder.LITTLE_ENDIAN)

            // Header (4 bytes)
            buffer.put(MAGIC_0)
            buffer.put(MAGIC_1)
            buffer.put(PROTOCOL_VERSION)
            buffer.put(packet.packetType)

            // Sequence & Timestamp (12 bytes)
            buffer.putInt(packet.sequenceNumber.toInt())
            buffer.putLong(packet.timestampUs)

            // Analog Axes (14 bytes)
            buffer.putShort(packet.steering)
            buffer.putShort((packet.throttle and 0xFFFF).toShort())
            buffer.putShort((packet.brake and 0xFFFF).toShort())
            buffer.putShort((packet.clutch and 0xFFFF).toShort())
            buffer.putShort((packet.handbrake and 0xFFFF).toShort())
            buffer.putShort(packet.joyX)
            buffer.putShort(packet.joyY)

            // Digital Buttons (8 bytes)
            buffer.putLong(packet.buttonsMask)

            // Aux Vehicles States (4 bytes)
            buffer.put(packet.gear)
            buffer.put(packet.wiperMode)
            buffer.put(packet.lightMode)
            buffer.put(packet.auxiliaryFlags)

            // Auth Tag / Security nonce (12 bytes)
            val tag = packet.authTag
            if (tag.size >= 12) {
                buffer.put(tag, 0, 12)
            } else {
                buffer.put(tag)
                for (i in tag.size until 12) buffer.put(0.toByte())
            }

            // CRC32 Checksum (4 bytes) over bytes 0..59
            val crc = CRC32()
            crc.update(buffer.array(), 0, 60)
            val checksum = crc.value
            buffer.putInt(checksum.toInt())

            return buffer.array()
        }

        fun deserialize(bytes: ByteArray): BazPacket? {
            if (bytes.size < PACKET_SIZE) return null
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

            val m0 = buffer.get()
            val m1 = buffer.get()
            if (m0 != MAGIC_0 || m1 != MAGIC_1) return null

            val version = buffer.get()
            val type = buffer.get()

            val seq = buffer.getInt().toLong() and 0xFFFFFFFFL
            val timestamp = buffer.getLong()

            val steering = buffer.getShort()
            val throttle = buffer.getShort().toInt() and 0xFFFF
            val brake = buffer.getShort().toInt() and 0xFFFF
            val clutch = buffer.getShort().toInt() and 0xFFFF
            val handbrake = buffer.getShort().toInt() and 0xFFFF
            val joyX = buffer.getShort()
            val joyY = buffer.getShort()

            val buttons = buffer.getLong()

            val gear = buffer.get()
            val wiper = buffer.get()
            val light = buffer.get()
            val aux = buffer.get()

            val authTag = ByteArray(12)
            buffer.get(authTag)

            val packetCrc = buffer.getInt().toLong() and 0xFFFFFFFFL

            // Validate CRC
            val crc = CRC32()
            crc.update(bytes, 0, 60)
            if (crc.value != packetCrc) {
                return null // Corrupted packet dropped
            }

            return BazPacket(
                packetType = type,
                sequenceNumber = seq,
                timestampUs = timestamp,
                steering = steering,
                throttle = throttle,
                brake = brake,
                clutch = clutch,
                handbrake = handbrake,
                joyX = joyX,
                joyY = joyY,
                buttonsMask = buttons,
                gear = gear,
                wiperMode = wiper,
                lightMode = light,
                auxiliaryFlags = aux,
                authTag = authTag,
                crc32 = packetCrc
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BazPacket

        if (packetType != other.packetType) return false
        if (sequenceNumber != other.sequenceNumber) return false
        if (timestampUs != other.timestampUs) return false
        if (steering != other.steering) return false
        if (throttle != other.throttle) return false
        if (brake != other.brake) return false
        if (clutch != other.clutch) return false
        if (handbrake != other.handbrake) return false
        if (joyX != other.joyX) return false
        if (joyY != other.joyY) return false
        if (buttonsMask != other.buttonsMask) return false
        if (gear != other.gear) return false
        if (wiperMode != other.wiperMode) return false
        if (lightMode != other.lightMode) return false
        if (auxiliaryFlags != other.auxiliaryFlags) return false
        if (!authTag.contentEquals(other.authTag)) return false
        if (crc32 != other.crc32) return false

        return true
    }

    override fun hashCode(): Int {
        var result = packetType.toInt()
        result = 31 * result + sequenceNumber.hashCode()
        result = 31 * result + timestampUs.hashCode()
        result = 31 * result + steering
        result = 31 * result + throttle
        result = 31 * result + brake
        result = 31 * result + clutch
        result = 31 * result + handbrake
        result = 31 * result + joyX
        result = 31 * result + joyY
        result = 31 * result + buttonsMask.hashCode()
        result = 31 * result + gear
        result = 31 * result + wiperMode
        result = 31 * result + lightMode
        result = 31 * result + auxiliaryFlags
        result = 31 * result + authTag.contentHashCode()
        result = 31 * result + crc32.hashCode()
        return result
    }
}
