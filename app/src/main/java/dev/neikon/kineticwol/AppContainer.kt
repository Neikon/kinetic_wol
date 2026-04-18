package dev.neikon.kineticwol

import android.content.Context
import dev.neikon.kineticwol.actions.DeviceShortcutPublisher
import dev.neikon.kineticwol.data.local.KineticWolDatabase
import dev.neikon.kineticwol.data.repository.RoomDeviceRepository
import dev.neikon.kineticwol.domain.shutdown.AgentShutdownSender
import dev.neikon.kineticwol.domain.repository.DeviceRepository
import dev.neikon.kineticwol.domain.wol.WakeOnLanSender
import dev.neikon.kineticwol.ui.home.HomeScreenPreferences

class AppContainer(context: Context) {
    private val database = KineticWolDatabase.create(context)
    private val appContext = context.applicationContext

    val deviceRepository: DeviceRepository = RoomDeviceRepository(database.wakeDeviceDao())
    val wakeOnLanSender: WakeOnLanSender = WakeOnLanSender()
    val agentShutdownSender = AgentShutdownSender()
    val deviceShortcutPublisher = DeviceShortcutPublisher(appContext)
    val homeScreenPreferences = HomeScreenPreferences(appContext)
}
