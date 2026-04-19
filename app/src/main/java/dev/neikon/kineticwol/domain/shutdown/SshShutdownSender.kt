package dev.neikon.kineticwol.domain.shutdown

import android.content.Context
import android.util.Log
import dev.neikon.kineticwol.domain.model.RemoteShutdownMethod
import dev.neikon.kineticwol.domain.model.SshShutdownConfig
import dev.neikon.kineticwol.domain.model.WakeDevice
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.PublicKey
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.SecurityUtils
import net.schmizz.sshj.common.SSHException
import net.schmizz.sshj.connection.ConnectionException
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.TransportException
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import net.schmizz.sshj.userauth.UserAuthException

data class SshStatusSuccess(
    val message: String,
    val hostKeyFingerprint: String,
)

sealed interface SshRequestFailure {
    data class InvalidConfiguration(val message: String?) : SshRequestFailure
    data class HostUnreachable(val message: String?) : SshRequestFailure
    data class ConnectionRefused(val message: String?) : SshRequestFailure
    data class AuthenticationFailed(val message: String?) : SshRequestFailure
    data class HostKeyMismatch(val message: String?) : SshRequestFailure
    data class InvalidPrivateKey(val message: String?) : SshRequestFailure
    data class CommandFailed(val message: String?) : SshRequestFailure
    data class Timeout(val message: String? = null) : SshRequestFailure
    data class Network(val message: String? = null) : SshRequestFailure
    data class Unknown(val message: String? = null) : SshRequestFailure
}

sealed interface SshStatusResult {
    data class Success(val data: SshStatusSuccess) : SshStatusResult
    data class Failure(val error: SshRequestFailure) : SshStatusResult
}

sealed interface SshPowerOffResult {
    data class Success(val message: String) : SshPowerOffResult
    data class Failure(val error: SshRequestFailure) : SshPowerOffResult
}

class SshShutdownSender(
    private val context: Context,
) {
    suspend fun checkStatus(config: SshShutdownConfig): SshStatusResult =
        withContext(Dispatchers.IO) {
            val validationFailure = validateConfig(config)
            if (validationFailure != null) {
                return@withContext SshStatusResult.Failure(validationFailure)
            }

            runCatching {
                runCommand(config, TEST_COMMAND)
            }.fold(
                onSuccess = { result ->
                    if (result.exitStatus == 0 && result.stdout.contains(TEST_TOKEN)) {
                        SshStatusResult.Success(
                            SshStatusSuccess(
                                message = "SSH authentication and remote command execution succeeded.",
                                hostKeyFingerprint = result.hostKeyFingerprint,
                            ),
                        )
                    } else {
                        SshStatusResult.Failure(
                            SshRequestFailure.CommandFailed(
                                result.stderr.ifBlank { result.stdout }.ifBlank {
                                    "Remote test command did not report success."
                                },
                            ),
                        )
                    }
                },
                onFailure = { error ->
                    SshStatusResult.Failure(error.toFailure())
                },
            )
        }

    suspend fun send(device: WakeDevice): SshPowerOffResult {
        val remoteShutdown = device.remoteShutdown
        val config = remoteShutdown.ssh

        require(remoteShutdown.enabled) { "Remote shutdown is disabled for this device." }
        require(remoteShutdown.method == RemoteShutdownMethod.SSH) {
            "Remote shutdown is not configured to use SSH."
        }
        require(config != null) { "Missing SSH shutdown configuration." }

        return send(config)
    }

    suspend fun send(config: SshShutdownConfig): SshPowerOffResult =
        withContext(Dispatchers.IO) {
            val validationFailure = validateConfig(config)
            if (validationFailure != null) {
                return@withContext SshPowerOffResult.Failure(validationFailure)
            }
            if (config.hostKeyFingerprint.trim().isBlank()) {
                return@withContext SshPowerOffResult.Failure(
                    SshRequestFailure.InvalidConfiguration(
                        "SSH host key fingerprint is blank.",
                    ),
                )
            }

            var commandStarted = false
            runCatching {
                runCommand(config, config.command.trim().ifBlank { SshShutdownConfig.DEFAULT_COMMAND }) {
                    commandStarted = true
                }
            }.fold(
                onSuccess = { result ->
                    if (result.exitStatus == null || result.exitStatus == 0) {
                        SshPowerOffResult.Success(
                            result.stderr.ifBlank { result.stdout }.ifBlank {
                                "SSH poweroff command was accepted."
                            },
                        )
                    } else {
                        SshPowerOffResult.Failure(
                            SshRequestFailure.CommandFailed(
                                result.stderr.ifBlank { result.stdout }.ifBlank {
                                    "SSH shutdown command exited with status ${result.exitStatus}."
                                },
                            ),
                        )
                    }
                },
                onFailure = { error ->
                    if (commandStarted && error.isLikelyConnectionClosedAfterCommand()) {
                        SshPowerOffResult.Success("SSH connection closed after the shutdown command was sent.")
                    } else {
                        SshPowerOffResult.Failure(error.toFailure())
                    }
                },
            )
        }

    internal fun validateConfig(config: SshShutdownConfig): SshRequestFailure.InvalidConfiguration? {
        if (config.host.trim().isBlank()) {
            return SshRequestFailure.InvalidConfiguration("SSH host is blank.")
        }
        if (config.port !in 1..65535) {
            return SshRequestFailure.InvalidConfiguration("SSH port must be between 1 and 65535.")
        }
        if (config.username.trim().isBlank()) {
            return SshRequestFailure.InvalidConfiguration("SSH username is blank.")
        }
        if (config.privateKey.trim().isBlank()) {
            return SshRequestFailure.InvalidConfiguration("SSH private key is blank.")
        }
        return null
    }

    private fun runCommand(
        config: SshShutdownConfig,
        command: String,
        onCommandStarted: (() -> Unit)? = null,
    ): SshCommandResult {
        val keyFile = writePrivateKeyToCache(config.privateKey)

        return try {
            SSHClient().use { client ->
                client.connectTimeout = CONNECT_TIMEOUT_MS
                client.timeout = SOCKET_TIMEOUT_MS
                val verifier = CapturingHostKeyVerifier(config.hostKeyFingerprint)
                client.addHostKeyVerifier(verifier)

                Log.d(TAG, "SSH connect ${config.username}@${config.host}:${config.port}")
                client.connect(config.host.trim(), config.port)

                val keyProvider =
                    if (config.keyPassphrase.isNullOrBlank()) {
                        client.loadKeys(keyFile.absolutePath)
                    } else {
                        client.loadKeys(keyFile.absolutePath, config.keyPassphrase.toCharArray())
                    }

                client.authPublickey(config.username.trim(), keyProvider)

                client.startSession().use { session ->
                    executeSessionCommand(
                        session = session,
                        command = command,
                        onCommandStarted = onCommandStarted,
                        hostKeyFingerprint = verifier.capturedFingerprint.orEmpty(),
                    )
                }
            }
        } finally {
            keyFile.delete()
        }
    }

    private fun executeSessionCommand(
        session: Session,
        command: String,
        hostKeyFingerprint: String,
        onCommandStarted: (() -> Unit)? = null,
    ): SshCommandResult {
        Log.d(TAG, "SSH exec $command")
        val remoteCommand = session.exec(command)
        onCommandStarted?.invoke()

        return remoteCommand.use { cmd ->
            cmd.join(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            val stdout = cmd.inputStream.bufferedReader().use { it.readText().trim() }
            val stderr = cmd.errorStream.bufferedReader().use { it.readText().trim() }
            cmd.close()
            val exitStatus = cmd.exitStatus
            Log.d(TAG, "SSH exec $command -> exitStatus=$exitStatus")
            SshCommandResult(
                exitStatus = exitStatus,
                stdout = stdout,
                stderr = stderr,
                hostKeyFingerprint = hostKeyFingerprint,
            )
        }
    }

    private fun writePrivateKeyToCache(privateKey: String): File =
        File.createTempFile("kineticwol-ssh-", ".key", context.cacheDir).apply {
            writeText(privateKey.trim())
            setReadable(true, true)
            setWritable(true, true)
        }

    private fun Throwable.toFailure(): SshRequestFailure =
        when (this) {
            is UnknownHostException -> SshRequestFailure.HostUnreachable(message)
            is ConnectException -> SshRequestFailure.ConnectionRefused(message)
            is SocketTimeoutException -> SshRequestFailure.Timeout(message)
            is UserAuthException -> SshRequestFailure.AuthenticationFailed(message)
            is TransportException -> {
                when {
                    message.containsAny("host key", "fingerprint", "verify") ->
                        SshRequestFailure.HostKeyMismatch(message)
                    else -> SshRequestFailure.Network(message)
                }
            }
            is ConnectionException -> SshRequestFailure.Network(message)
            is SSHException -> {
                when {
                    message.containsAny("bouncycastle", "key provider", "private key", "passphrase") ->
                        SshRequestFailure.InvalidPrivateKey(message)
                    message.containsAny("host key", "fingerprint", "verify") ->
                        SshRequestFailure.HostKeyMismatch(message)
                    else -> SshRequestFailure.Unknown(message)
                }
            }
            is IOException -> SshRequestFailure.Network(message)
            else -> SshRequestFailure.Unknown(message)
        }

    private fun Throwable.isLikelyConnectionClosedAfterCommand(): Boolean =
        this is EOFException ||
            (this is SocketException &&
                message.containsAny(
                    "broken pipe",
                    "connection reset",
                    "socket closed",
                    "connection abort",
                )) ||
            (this is TransportException &&
                message.containsAny(
                    "connection reset",
                    "connection lost",
                    "socket closed",
                    "disconnect",
                    "EOF",
                ))

    private fun String?.containsAny(vararg needles: String): Boolean {
        val haystack = this?.lowercase().orEmpty()
        return needles.any { haystack.contains(it.lowercase()) }
    }

    private data class SshCommandResult(
        val exitStatus: Int?,
        val stdout: String,
        val stderr: String,
        val hostKeyFingerprint: String,
    )

    private class CapturingHostKeyVerifier(
        expectedFingerprint: String,
    ) : HostKeyVerifier {
        private val normalizedExpectedFingerprint = expectedFingerprint.normalizeFingerprint()

        var capturedFingerprint: String? = null
            private set

        override fun verify(
            hostname: String,
            port: Int,
            key: PublicKey,
        ): Boolean {
            val actualFingerprint = SecurityUtils.getFingerprint(key)
            capturedFingerprint = actualFingerprint

            return normalizedExpectedFingerprint.isEmpty() ||
                normalizedExpectedFingerprint == actualFingerprint.normalizeFingerprint()
        }

        override fun findExistingAlgorithms(
            p0: String,
            p1: Int,
        ): MutableList<String> = mutableListOf()
    }

    companion object {
        private const val TAG = "SshShutdownSender"
        private const val CONNECT_TIMEOUT_MS = 5_000
        private const val SOCKET_TIMEOUT_MS = 5_000
        private const val COMMAND_TIMEOUT_SECONDS = 5L
        private const val TEST_TOKEN = "kineticwol-ssh-ok"
        private const val TEST_COMMAND = "printf '$TEST_TOKEN'"
    }
}

private fun String.normalizeFingerprint(): String =
    trim().lowercase().replace("sha256:", "")
