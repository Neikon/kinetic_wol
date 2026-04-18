package dev.neikon.kineticwol.data.local

import dev.neikon.kineticwol.domain.model.AgentShutdownConfig
import dev.neikon.kineticwol.domain.model.RemoteShutdownConfig
import dev.neikon.kineticwol.domain.model.RemoteShutdownMethod
import dev.neikon.kineticwol.domain.model.WakeDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeDeviceEntityMappingTest {
    @Test
    fun `toEntity persists agent shutdown fields`() {
        val device =
            WakeDevice(
                id = "desktop",
                name = "Desktop",
                macAddress = "AA:BB:CC:DD:EE:FF",
                host = "192.168.1.255",
                port = 9,
                remoteShutdown =
                    RemoteShutdownConfig(
                        enabled = true,
                        method = RemoteShutdownMethod.AGENT,
                        agent =
                            AgentShutdownConfig(
                                baseUrl = "https://desktop.local:8787",
                                authToken = "secret-token",
                            ),
                    ),
            )

        val entity = device.toEntity()

        assertTrue(entity.shutdownEnabled)
        assertEquals(RemoteShutdownMethod.AGENT.name, entity.shutdownMethod)
        assertEquals("https://desktop.local:8787", entity.shutdownAgentBaseUrl)
        assertEquals("secret-token", entity.shutdownAgentAuthToken)
    }

    @Test
    fun `toDomain rebuilds ready agent shutdown config`() {
        val entity =
            WakeDeviceEntity(
                id = "desktop",
                name = "Desktop",
                normalizedName = "desktop",
                macAddress = "AA:BB:CC:DD:EE:FF",
                host = "192.168.1.255",
                port = 9,
                shutdownEnabled = true,
                shutdownMethod = RemoteShutdownMethod.AGENT.name,
                shutdownAgentBaseUrl = "https://desktop.local:8787",
                shutdownAgentAuthToken = "secret-token",
            )

        val device = entity.toDomain()

        assertTrue(device.remoteShutdown.enabled)
        assertTrue(device.remoteShutdown.isReady)
        assertEquals(RemoteShutdownMethod.AGENT, device.remoteShutdown.method)
        assertEquals("https://desktop.local:8787", device.remoteShutdown.agent?.baseUrl)
        assertEquals("secret-token", device.remoteShutdown.agent?.authToken)
    }

    @Test
    fun `toDomain keeps shutdown disabled when agent config is missing`() {
        val entity =
            WakeDeviceEntity(
                id = "desktop",
                name = "Desktop",
                normalizedName = "desktop",
                macAddress = "AA:BB:CC:DD:EE:FF",
                host = "192.168.1.255",
                port = 9,
                shutdownEnabled = false,
                shutdownMethod = null,
                shutdownAgentBaseUrl = null,
                shutdownAgentAuthToken = null,
            )

        val device = entity.toDomain()

        assertFalse(device.remoteShutdown.enabled)
        assertFalse(device.remoteShutdown.isReady)
    }
}
