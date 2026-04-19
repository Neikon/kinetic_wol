package dev.neikon.kineticwol.domain.shutdown

import dev.neikon.kineticwol.domain.model.SshShutdownConfig
import android.test.mock.MockContext
import org.junit.Assert.assertEquals
import org.junit.Test

class SshShutdownSenderTest {
    private val sender = SshShutdownSender(MockContext())

    @Test
    fun `validateConfig accepts complete ssh configuration`() {
        assertEquals(
            null,
            sender.validateConfig(
                SshShutdownConfig(
                    host = "192.168.1.20",
                    port = 22,
                    username = "neikon",
                    privateKey = "-----BEGIN OPENSSH PRIVATE KEY-----",
                    hostKeyFingerprint = "SHA256:test",
                ),
            ),
        )
    }

    @Test
    fun `validateConfig rejects empty host`() {
        assertEquals(
            "SSH host is blank.",
            sender.validateConfig(
                SshShutdownConfig(
                    host = "",
                    port = 22,
                    username = "neikon",
                    privateKey = "key",
                    hostKeyFingerprint = "SHA256:test",
                ),
            )?.message,
        )
    }

    @Test
    fun `default ssh command uses non interactive sudo`() {
        assertEquals(
            "sudo -n systemctl poweroff",
            SshShutdownConfig.DEFAULT_COMMAND,
        )
    }
}
