package dev.neikon.kineticwol

import android.content.Context
import dev.neikon.kineticwol.data.local.KineticWolDatabase
import dev.neikon.kineticwol.data.repository.RoomDeviceRepository
import dev.neikon.kineticwol.domain.repository.DeviceRepository
import dev.neikon.kineticwol.domain.wol.WakeOnLanSender

class AppContainer(context: Context) {
    private val database = KineticWolDatabase.create(context)

    val deviceRepository: DeviceRepository = RoomDeviceRepository(database.wakeDeviceDao())
    val wakeOnLanSender: WakeOnLanSender = WakeOnLanSender()
}
