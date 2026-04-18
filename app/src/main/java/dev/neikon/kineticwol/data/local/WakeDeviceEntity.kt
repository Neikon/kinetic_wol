package dev.neikon.kineticwol.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.neikon.kineticwol.domain.model.AgentShutdownConfig
import dev.neikon.kineticwol.domain.model.RemoteShutdownConfig
import dev.neikon.kineticwol.domain.model.RemoteShutdownMethod
import dev.neikon.kineticwol.domain.model.WakeDevice
import dev.neikon.kineticwol.util.normalizeDeviceName

@Entity(tableName = "wake_devices")
data class WakeDeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "normalized_name") val normalizedName: String,
    @ColumnInfo(name = "mac_address") val macAddress: String,
    val host: String,
    val port: Int,
    @ColumnInfo(name = "shutdown_enabled") val shutdownEnabled: Boolean,
    @ColumnInfo(name = "shutdown_method") val shutdownMethod: String?,
    @ColumnInfo(name = "shutdown_agent_base_url") val shutdownAgentBaseUrl: String?,
    @ColumnInfo(name = "shutdown_agent_auth_token") val shutdownAgentAuthToken: String?,
)

fun WakeDeviceEntity.toDomain(): WakeDevice =
    WakeDevice(
        id = id,
        name = name,
        macAddress = macAddress,
        host = host,
        port = port,
        remoteShutdown =
            RemoteShutdownConfig(
                enabled = shutdownEnabled,
                method = shutdownMethod.toRemoteShutdownMethod(),
                agent =
                    if (
                        shutdownEnabled &&
                        !shutdownAgentBaseUrl.isNullOrBlank() &&
                        !shutdownAgentAuthToken.isNullOrBlank()
                    ) {
                        AgentShutdownConfig(
                            baseUrl = shutdownAgentBaseUrl,
                            authToken = shutdownAgentAuthToken,
                        )
                    } else {
                        null
                    },
            ),
    )

fun WakeDevice.toEntity(): WakeDeviceEntity =
    WakeDeviceEntity(
        id = id,
        name = name,
        normalizedName = normalizeDeviceName(name),
        macAddress = macAddress,
        host = host,
        port = port,
        shutdownEnabled = remoteShutdown.enabled,
        shutdownMethod = remoteShutdown.method.name,
        shutdownAgentBaseUrl = remoteShutdown.agent?.baseUrl,
        shutdownAgentAuthToken = remoteShutdown.agent?.authToken,
    )

private fun String?.toRemoteShutdownMethod(): RemoteShutdownMethod =
    runCatching {
        if (isNullOrBlank()) {
            RemoteShutdownMethod.AGENT
        } else {
            RemoteShutdownMethod.valueOf(this)
        }
    }.getOrDefault(RemoteShutdownMethod.AGENT)
