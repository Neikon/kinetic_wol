package dev.neikon.kineticwol.domain.wol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeOnLanSenderTest {
    private val sender = WakeOnLanSender()

    @Test
    fun `normalizeMac accepts colon format`() {
        assertEquals("AABBCCDDEEFF", sender.normalizeMac("AA:BB:CC:DD:EE:FF"))
    }

    @Test
    fun `normalizeMac accepts dash format`() {
        assertEquals("AABBCCDDEEFF", sender.normalizeMac("AA-BB-CC-DD-EE-FF"))
    }

    @Test
    fun `normalizeMac accepts compact format`() {
        assertEquals("AABBCCDDEEFF", sender.normalizeMac("aabbccddeeff"))
    }

    @Test
    fun `buildMagicPacket uses expected size and preamble`() {
        val packet = sender.buildMagicPacket("AA:BB:CC:DD:EE:FF")

        assertEquals(102, packet.size)
        assertTrue(packet.take(6).all { it == 0xFF.toByte() })
    }

    @Test
    fun `buildMagicPacket repeats mac sixteen times`() {
        val packet = sender.buildMagicPacket("AA:BB:CC:DD:EE:FF")
        val expectedMac = byteArrayOf(
            0xAA.toByte(),
            0xBB.toByte(),
            0xCC.toByte(),
            0xDD.toByte(),
            0xEE.toByte(),
            0xFF.toByte(),
        )

        repeat(16) { repetition ->
            val start = 6 + (repetition * 6)
            val slice = packet.copyOfRange(start, start + 6)
            assertTrue(slice.contentEquals(expectedMac))
        }
    }
}
