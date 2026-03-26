package dev.neikon.kineticwol.actions

import android.app.PendingIntent
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import dev.neikon.kineticwol.KineticWolApplication
import dev.neikon.kineticwol.MainActivity
import dev.neikon.kineticwol.R
import dev.neikon.kineticwol.domain.model.WakeDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WakeQuickTileService : TileService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val appContainer
        get() = (application as KineticWolApplication).appContainer

    override fun onStartListening() {
        super.onStartListening()
        serviceScope.launch {
            refreshTile()
        }
    }

    override fun onClick() {
        super.onClick()

        serviceScope.launch {
            val devices = appContainer.deviceRepository.observeDevices().first()

            when {
                devices.isEmpty() -> openMainApp()
                devices.size == 1 -> wakeDevice(devices.single())
                else -> openDevicePicker()
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun refreshTile() {
        val tile = qsTile ?: return
        val devices = appContainer.deviceRepository.observeDevices().first()

        tile.label = getString(R.string.quick_tile_label)
        tile.state = if (devices.isEmpty()) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        tile.subtitle = when {
            devices.isEmpty() -> getString(R.string.quick_tile_subtitle_empty)
            devices.size == 1 -> devices.single().name
            else -> getString(R.string.quick_tile_subtitle_many, devices.size)
        }
        tile.updateTile()
    }

    private suspend fun wakeDevice(device: WakeDevice) {
        runCatching {
            appContainer.wakeOnLanSender.send(device)
        }.onSuccess {
            appContainer.deviceShortcutPublisher.reportUsed(device.id)
            Toast.makeText(
                this,
                getString(R.string.manual_wake_success, device.name),
                Toast.LENGTH_SHORT,
            ).show()
            refreshTile()
        }.onFailure {
            Toast.makeText(
                this,
                getString(R.string.manual_wake_error, device.name),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun openMainApp() {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        startActivityAndCollapse(pendingIntent)
    }

    private fun openDevicePicker() {
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, QuickTileDevicePickerActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        startActivityAndCollapse(pendingIntent)
    }
}
