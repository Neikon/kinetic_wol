package dev.neikon.kineticwol.data.repository

import dev.neikon.kineticwol.data.local.WakeDeviceDao
import dev.neikon.kineticwol.data.local.toDomain
import dev.neikon.kineticwol.data.local.toEntity
import dev.neikon.kineticwol.domain.model.WakeDevice
import dev.neikon.kineticwol.domain.repository.DeviceRepository
import dev.neikon.kineticwol.util.normalizeDeviceName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomDeviceRepository(
    private val dao: WakeDeviceDao,
) : DeviceRepository {
    override fun observeDevices(): Flow<List<WakeDevice>> =
        dao.observeAll().map { devices -> devices.map { it.toDomain() } }

    override suspend fun upsert(device: WakeDevice) {
        dao.upsert(device.toEntity())
    }

    override suspend fun delete(deviceId: String) {
        dao.deleteById(deviceId)
    }

    override suspend fun findByName(name: String): WakeDevice? =
        dao.findByNormalizedName(normalizeDeviceName(name))?.toDomain()
}
