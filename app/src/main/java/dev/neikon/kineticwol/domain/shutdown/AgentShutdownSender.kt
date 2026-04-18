package dev.neikon.kineticwol.domain.shutdown

import dev.neikon.kineticwol.domain.model.AgentShutdownConfig
import dev.neikon.kineticwol.domain.model.RemoteShutdownMethod
import dev.neikon.kineticwol.domain.model.WakeDevice
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AgentShutdownSender {
    suspend fun send(device: WakeDevice) {
        val remoteShutdown = device.remoteShutdown
        val config = remoteShutdown.agent

        require(remoteShutdown.enabled) { "Remote shutdown is disabled for this device." }
        require(remoteShutdown.method == RemoteShutdownMethod.AGENT) {
            "Remote shutdown is not configured to use the agent."
        }
        require(config != null) { "Missing agent shutdown configuration." }

        send(config)
    }

    suspend fun send(config: AgentShutdownConfig) {
        withContext(Dispatchers.IO) {
            val connection =
                (URL(powerOffUrl(config.baseUrl)).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    doOutput = true
                    setRequestProperty("Authorization", "Bearer ${config.authToken.trim()}")
                    setRequestProperty("Content-Type", "application/json")
                }

            try {
                connection.outputStream.use { output ->
                    output.write("{}".encodeToByteArray())
                }

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    val responseBody = readError(connection)
                    throw IOException(
                        "Agent poweroff request failed with HTTP $responseCode" +
                            if (responseBody.isNotBlank()) ": $responseBody" else "",
                    )
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    internal fun powerOffUrl(baseUrl: String): String =
        baseUrl.trim().removeSuffix("/") + POWER_OFF_PATH

    private fun readError(connection: HttpURLConnection): String =
        runCatching {
            connection.errorStream?.bufferedReader()?.use { it.readText().trim() }.orEmpty()
        }.getOrDefault("")

    companion object {
        private const val POWER_OFF_PATH = "/v1/poweroff"
        private const val TIMEOUT_MS = 5_000
    }
}
