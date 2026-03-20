package dev.neikon.kineticwol.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
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
)

fun WakeDeviceEntity.toDomain(): WakeDevice =
    WakeDevice(
        id = id,
        name = name,
        macAddress = macAddress,
        host = host,
        port = port,
    )

fun WakeDevice.toEntity(): WakeDeviceEntity =
    WakeDeviceEntity(
        id = id,
        name = name,
        normalizedName = normalizeDeviceName(name),
        macAddress = macAddress,
        host = host,
        port = port,
    )
