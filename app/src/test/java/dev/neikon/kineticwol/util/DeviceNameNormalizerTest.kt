package dev.neikon.kineticwol.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceNameNormalizerTest {
    @Test
    fun `normalizeDeviceName trims and lowercases`() {
        assertEquals(
            "gaming pc",
            normalizeDeviceName("  Gaming PC  "),
        )
    }
}
