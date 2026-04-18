package dev.neikon.kineticwol.domain.shutdown

import android.util.Log
import dev.neikon.kineticwol.domain.model.AgentShutdownConfig
import dev.neikon.kineticwol.domain.model.RemoteShutdownMethod
import dev.neikon.kineticwol.domain.model.WakeDevice
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class AgentStatusSuccess(
    val canPowerOff: String,
    val message: String,
)

sealed interface AgentRequestFailure {
    data class InvalidBaseUrl(val message: String?) : AgentRequestFailure
    data class HostUnreachable(val message: String?) : AgentRequestFailure
    data class ConnectionRefused(val message: String?) : AgentRequestFailure
    data class Unauthorized(val message: String?) : AgentRequestFailure
    data class NotFound(val message: String?) : AgentRequestFailure
    data class BackendUnavailable(val message: String?) : AgentRequestFailure
    data class Timeout(val message: String? = null) : AgentRequestFailure
    data class CleartextBlocked(val message: String? = null) : AgentRequestFailure
    data class Ssl(val message: String? = null) : AgentRequestFailure
    data class Network(val message: String? = null) : AgentRequestFailure
    data class Unknown(val message: String? = null) : AgentRequestFailure
}

sealed interface AgentStatusResult {
    data class Success(val data: AgentStatusSuccess) : AgentStatusResult
    data class Failure(val error: AgentRequestFailure) : AgentStatusResult
}

sealed interface AgentPowerOffResult {
    data class Success(val message: String) : AgentPowerOffResult
    data class Failure(val error: AgentRequestFailure) : AgentPowerOffResult
}

class AgentShutdownSender {
    suspend fun checkStatus(config: AgentShutdownConfig): AgentStatusResult =
        withContext(Dispatchers.IO) {
            val baseUrlFailure = validateBaseUrl(config.baseUrl)
            if (baseUrlFailure != null) {
                return@withContext AgentStatusResult.Failure(baseUrlFailure)
            }
            val normalizedBaseUrl = normalizeBaseUrl(config.baseUrl)
            runCatching {
                request(
                    url = statusUrl(normalizedBaseUrl),
                    authToken = config.authToken,
                    method = "GET",
                    body = null,
                )
            }.fold(
                onSuccess = { response ->
                    when (response.code) {
                        HTTP_OK -> {
                            val json = response.body.toJsonObject()
                            AgentStatusResult.Success(
                                AgentStatusSuccess(
                                    canPowerOff = json.optString("canPowerOff", ""),
                                    message = json.optString("message", ""),
                                ),
                            )
                        }

                        else -> AgentStatusResult.Failure(response.toFailure())
                    }
                },
                onFailure = { throwable ->
                    AgentStatusResult.Failure(throwable.toFailure())
                },
            )
        }

    suspend fun send(device: WakeDevice): AgentPowerOffResult {
        val remoteShutdown = device.remoteShutdown
        val config = remoteShutdown.agent

        require(remoteShutdown.enabled) { "Remote shutdown is disabled for this device." }
        require(remoteShutdown.method == RemoteShutdownMethod.AGENT) {
            "Remote shutdown is not configured to use the agent."
        }
        require(config != null) { "Missing agent shutdown configuration." }

        return send(config)
    }

    suspend fun send(config: AgentShutdownConfig): AgentPowerOffResult =
        withContext(Dispatchers.IO) {
            val baseUrlFailure = validateBaseUrl(config.baseUrl)
            if (baseUrlFailure != null) {
                return@withContext AgentPowerOffResult.Failure(baseUrlFailure)
            }
            val normalizedBaseUrl = normalizeBaseUrl(config.baseUrl)
            runCatching {
                request(
                    url = powerOffUrl(normalizedBaseUrl),
                    authToken = config.authToken,
                    method = "POST",
                    body = "{}",
                )
            }.fold(
                onSuccess = { response ->
                    when (response.code) {
                        HTTP_OK -> {
                            val json = response.body.toJsonObject()
                            AgentPowerOffResult.Success(
                                message = json.optString("message", ""),
                            )
                        }

                        else -> AgentPowerOffResult.Failure(response.toFailure())
                    }
                },
                onFailure = { throwable ->
                    AgentPowerOffResult.Failure(throwable.toFailure())
                },
            )
        }

    internal fun statusUrl(baseUrl: String): String =
        baseUrl.trim().removeSuffix("/") + STATUS_PATH

    internal fun powerOffUrl(baseUrl: String): String =
        baseUrl.trim().removeSuffix("/") + POWER_OFF_PATH

    internal fun normalizeBaseUrl(baseUrl: String): String =
        baseUrl.trim().removeSuffix("/")

    internal fun validateBaseUrl(baseUrl: String): AgentRequestFailure.InvalidBaseUrl? {
        val trimmed = baseUrl.trim()
        if (trimmed.isBlank()) {
            return AgentRequestFailure.InvalidBaseUrl("Base URL is blank.")
        }

        val uri =
            try {
                URI(trimmed)
            } catch (error: URISyntaxException) {
                return AgentRequestFailure.InvalidBaseUrl(error.message)
            }

        val scheme = uri.scheme?.lowercase()
        if (scheme !in SUPPORTED_SCHEMES) {
            return AgentRequestFailure.InvalidBaseUrl(
                "Base URL must start with http:// or https://",
            )
        }

        if (uri.host.isNullOrBlank()) {
            return AgentRequestFailure.InvalidBaseUrl("Base URL must include a host.")
        }

        if (uri.rawPath?.let { it.isNotBlank() && it != "/" } == true) {
            return AgentRequestFailure.InvalidBaseUrl("Base URL must not include a path.")
        }

        if (uri.rawQuery != null || uri.rawFragment != null) {
            return AgentRequestFailure.InvalidBaseUrl("Base URL must not include query or fragment.")
        }

        return null
    }

    private fun request(
        url: String,
        authToken: String,
        method: String,
        body: String?,
    ): HttpResponse {
        Log.d(TAG, "HTTP $method $url")
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Authorization", "Bearer ${authToken.trim()}")
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }

        return try {
            if (body != null) {
                connection.outputStream.use { output ->
                    output.write(body.encodeToByteArray())
                }
            }

            val responseCode = connection.responseCode
            val responseBody = readBody(connection, responseCode)
            Log.d(TAG, "HTTP $method $url -> $responseCode")
            HttpResponse(
                code = responseCode,
                body = responseBody,
            )
        } catch (error: Throwable) {
            Log.e(
                TAG,
                "HTTP $method $url failed with ${error::class.java.name}: ${error.message}",
                error,
            )
            throw error
        } finally {
            connection.disconnect()
        }
    }

    private fun readBody(
        connection: HttpURLConnection,
        responseCode: Int,
    ): String =
        runCatching {
            val stream =
                if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
            stream?.bufferedReader()?.use { it.readText().trim() }.orEmpty()
        }.getOrDefault("")

    private fun HttpResponse.toFailure(): AgentRequestFailure {
        val message = body.toJsonObjectOrNull()?.optString("message", null)
        return when (code) {
            HTTP_UNAUTHORIZED -> AgentRequestFailure.Unauthorized(message)
            HTTP_NOT_FOUND -> AgentRequestFailure.NotFound(message)
            HTTP_UNAVAILABLE -> AgentRequestFailure.BackendUnavailable(message)
            else -> AgentRequestFailure.Unknown(message)
        }
    }

    private fun Throwable.toFailure(): AgentRequestFailure =
        when (this) {
            is MalformedURLException -> AgentRequestFailure.InvalidBaseUrl(message)
            is SocketTimeoutException -> AgentRequestFailure.Timeout(message)
            is UnknownHostException -> AgentRequestFailure.HostUnreachable(message)
            is ConnectException -> AgentRequestFailure.ConnectionRefused(message)
            is SSLHandshakeException, is SSLException -> AgentRequestFailure.Ssl(message)
            is IOException ->
                if (message.orEmpty().contains("cleartext", ignoreCase = true)) {
                    AgentRequestFailure.CleartextBlocked(message)
                } else {
                    AgentRequestFailure.Network(message)
                }
            else -> AgentRequestFailure.Unknown(message)
        }

    private fun String.toJsonObject(): JSONObject =
        if (isBlank()) {
            JSONObject()
        } else {
            JSONObject(this)
        }

    private fun String.toJsonObjectOrNull(): JSONObject? =
        runCatching { toJsonObject() }.getOrNull()

    private data class HttpResponse(
        val code: Int,
        val body: String,
    )

    companion object {
        private const val TAG = "AgentShutdownSender"
        private const val STATUS_PATH = "/api/v1/status"
        private const val POWER_OFF_PATH = "/api/v1/poweroff"
        private const val TIMEOUT_MS = 5_000
        private val SUPPORTED_SCHEMES = setOf("http", "https")

        private const val HTTP_OK = 200
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_NOT_FOUND = 404
        private const val HTTP_UNAVAILABLE = 503
    }
}
