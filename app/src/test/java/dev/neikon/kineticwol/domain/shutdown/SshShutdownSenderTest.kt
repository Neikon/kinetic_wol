package dev.neikon.kineticwol.domain.shutdown

import android.test.mock.MockContext
import dev.neikon.kineticwol.domain.model.SshShutdownConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SshShutdownSenderTest {
    private val sender = SshShutdownSender(MockContext())
    private val keyGenerator = SshKeyMaterialGenerator()

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
                    hostKeyFingerprint = "aa:bb:cc:dd",
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
                    hostKeyFingerprint = "aa:bb:cc:dd",
                ),
            )?.message,
        )
    }

    @Test
    fun `validateConfig allows blank fingerprint for first trust on first use test`() {
        assertEquals(
            null,
            sender.validateConfig(
                SshShutdownConfig(
                    host = "192.168.1.20",
                    port = 22,
                    username = "neikon",
                    privateKey = "key",
                    hostKeyFingerprint = "",
                ),
            ),
        )
    }

    @Test
    fun `default ssh command uses non interactive sudo`() {
        assertEquals(
            "sudo -n systemctl poweroff",
            SshShutdownConfig.DEFAULT_COMMAND,
        )
    }

    @Test
    fun `generated key pair includes pem private key and authorized public key`() {
        val generated = keyGenerator.generate()

        assertTrue(generated.privateKeyPem.startsWith("-----BEGIN PRIVATE KEY-----"))
        assertTrue(generated.publicKeyAuthorized.startsWith("ssh-rsa "))
    }
}
