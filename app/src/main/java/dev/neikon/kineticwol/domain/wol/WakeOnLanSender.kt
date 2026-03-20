package dev.neikon.kineticwol.domain.wol

import dev.neikon.kineticwol.domain.model.WakeDevice
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WakeOnLanSender {
    suspend fun send(device: WakeDevice) = withContext(Dispatchers.IO) {
        require(device.port in 1..65535) { "Invalid port: ${device.port}" }

        val packetData = buildMagicPacket(device.macAddress)
        val address = InetAddress.getByName(device.host)

        DatagramSocket().use { socket ->
            socket.broadcast = true
            socket.send(
                DatagramPacket(
                    packetData,
                    packetData.size,
                    address,
                    device.port,
                ),
            )
        }
    }

    fun buildMagicPacket(macAddress: String): ByteArray {
        val normalizedMac = normalizeMac(macAddress)
        val macBytes = normalizedMac.chunked(2)
            .map { chunk -> chunk.toInt(16).toByte() }

        return ByteArray(MAGIC_PACKET_SIZE).apply {
            repeat(PREAMBLE_SIZE) { index ->
                this[index] = 0xFF.toByte()
            }

            repeat(MAC_REPETITIONS) { repetition ->
                macBytes.forEachIndexed { index, byte ->
                    this[PREAMBLE_SIZE + (repetition * MAC_BYTES) + index] = byte
                }
            }
        }
    }

    fun normalizeMac(macAddress: String): String {
        val normalized = macAddress
            .replace(":", "")
            .replace("-", "")
            .trim()
            .uppercase()

        require(normalized.length == MAC_BYTES * 2) {
            "MAC address must have exactly 12 hexadecimal characters"
        }
        require(normalized.all { it in HEX_DIGITS }) {
            "MAC address contains invalid hexadecimal digits"
        }

        return normalized
    }

    companion object {
        private const val PREAMBLE_SIZE = 6
        private const val MAC_BYTES = 6
        private const val MAC_REPETITIONS = 16
        private const val MAGIC_PACKET_SIZE = PREAMBLE_SIZE + (MAC_REPETITIONS * MAC_BYTES)
        private val HEX_DIGITS = ('0'..'9') + ('A'..'F')
    }
}
