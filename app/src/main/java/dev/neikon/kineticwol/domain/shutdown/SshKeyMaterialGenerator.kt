package dev.neikon.kineticwol.domain.shutdown

import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.util.Base64

data class GeneratedSshKeyPair(
    val privateKeyPem: String,
    val publicKeyAuthorized: String,
)

class SshKeyMaterialGenerator {
    fun generate(): GeneratedSshKeyPair {
        val generator = KeyPairGenerator.getInstance(KEY_ALGORITHM).apply {
            initialize(KEY_SIZE_BITS)
        }
        val keyPair = generator.generateKeyPair()
        val publicKey = keyPair.public as RSAPublicKey

        return GeneratedSshKeyPair(
            privateKeyPem = keyPair.private.encoded.toPem(PRIVATE_KEY_HEADER, PRIVATE_KEY_FOOTER),
            publicKeyAuthorized = publicKey.toAuthorizedKey(),
        )
    }

    private fun RSAPublicKey.toAuthorizedKey(): String {
        val payload = ByteArrayOutputStream().apply {
            writeSshString(SSH_KEY_TYPE)
            writeMpint(publicExponent)
            writeMpint(modulus)
        }.toByteArray()

        val base64 = Base64.getEncoder().encodeToString(payload)
        return "$SSH_KEY_TYPE $base64 $KEY_COMMENT"
    }

    private fun ByteArrayOutputStream.writeSshString(value: String) {
        val bytes = value.encodeToByteArray()
        writeInt(bytes.size)
        write(bytes)
    }

    private fun ByteArrayOutputStream.writeMpint(value: BigInteger) {
        val bytes = value.toByteArray()
        writeInt(bytes.size)
        write(bytes)
    }

    private fun ByteArrayOutputStream.writeInt(value: Int) {
        write((value ushr 24) and 0xFF)
        write((value ushr 16) and 0xFF)
        write((value ushr 8) and 0xFF)
        write(value and 0xFF)
    }

    private fun ByteArray.toPem(
        header: String,
        footer: String,
    ): String {
        val encoded = Base64.getEncoder().encodeToString(this)
        val chunks = encoded.chunked(PEM_LINE_WIDTH)
        return buildString {
            appendLine(header)
            chunks.forEach { appendLine(it) }
            append(footer)
        }
    }

    companion object {
        private const val KEY_ALGORITHM = "RSA"
        private const val KEY_SIZE_BITS = 3072
        private const val SSH_KEY_TYPE = "ssh-rsa"
        private const val KEY_COMMENT = "kinetic-wol@android"
        private const val PEM_LINE_WIDTH = 64
        private const val PRIVATE_KEY_HEADER = "-----BEGIN PRIVATE KEY-----"
        private const val PRIVATE_KEY_FOOTER = "-----END PRIVATE KEY-----"
    }
}
