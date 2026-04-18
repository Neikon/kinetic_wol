package dev.neikon.kineticwol.domain.shutdown

import org.junit.Assert.assertEquals
import org.junit.Test

class AgentShutdownSenderTest {
    private val sender = AgentShutdownSender()

    @Test
    fun `statusUrl appends canonical status endpoint`() {
        assertEquals(
            "https://desktop.local:8787/api/v1/status",
            sender.statusUrl("https://desktop.local:8787"),
        )
    }

    @Test
    fun `statusUrl trims base url and removes trailing slash`() {
        assertEquals(
            "https://desktop.local:8787/api/v1/status",
            sender.statusUrl(" https://desktop.local:8787/ "),
        )
    }

    @Test
    fun `powerOffUrl appends endpoint when base url has no trailing slash`() {
        assertEquals(
            "https://desktop.local:8787/api/v1/poweroff",
            sender.powerOffUrl("https://desktop.local:8787"),
        )
    }

    @Test
    fun `powerOffUrl trims base url and removes trailing slash`() {
        assertEquals(
            "https://desktop.local:8787/api/v1/poweroff",
            sender.powerOffUrl(" https://desktop.local:8787/ "),
        )
    }
}
