package dev.neikon.kineticwol.domain.repository

import dev.neikon.kineticwol.domain.model.WakeDevice
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    suspend fun upsert(device: WakeDevice)
    suspend fun delete(deviceId: String)
    suspend fun findByName(name: String): WakeDevice?
    fun observeDevices(): Flow<List<WakeDevice>>
}
