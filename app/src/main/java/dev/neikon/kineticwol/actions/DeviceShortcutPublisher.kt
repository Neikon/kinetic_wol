package dev.neikon.kineticwol.actions

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import dev.neikon.kineticwol.MainActivity
import dev.neikon.kineticwol.R
import dev.neikon.kineticwol.domain.model.WakeDevice

class DeviceShortcutPublisher(
    private val context: Context,
) {
    fun sync(devices: List<WakeDevice>) {
        val shortcuts = devices.mapIndexed { index, device ->
            buildShortcut(device = device, rank = index)
        }
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }

    fun remove(deviceId: String) {
        val shortcutId = shortcutId(deviceId)
        ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(shortcutId))
        ShortcutManagerCompat.removeLongLivedShortcuts(context, listOf(shortcutId))
    }

    fun reportUsed(deviceId: String) {
        ShortcutManagerCompat.reportShortcutUsed(context, shortcutId(deviceId))
    }

    private fun buildShortcut(
        device: WakeDevice,
        rank: Int,
    ): ShortcutInfoCompat {
        val intent = Intent(context, WakeDeviceActivity::class.java).apply {
            action = ACTION_WAKE_DEVICE
            putExtra(EXTRA_DEVICE_NAME, device.name)
        }

        return ShortcutInfoCompat.Builder(context, shortcutId(device.id))
            .setActivity(ComponentName(context, MainActivity::class.java))
            .setShortLabel(device.name)
            .setLongLabel(
                context.getString(R.string.shortcut_wake_device_long_label, device.name),
            )
            .setIntent(intent)
            .setRank(rank)
            .setLongLived(true)
            .addCapabilityBinding(CAPABILITY_WAKE_DEVICE)
            .addCapabilityBinding(
                CAPABILITY_WAKE_DEVICE,
                SHORTCUT_PARAMETER_DEVICE_NAME,
                listOf(device.name),
            )
            .build()
    }

    private fun shortcutId(deviceId: String): String = "wake_device_$deviceId"

    companion object {
        const val ACTION_WAKE_DEVICE = "dev.neikon.kineticwol.action.WAKE_DEVICE"
        const val EXTRA_DEVICE_NAME = "deviceName"
        private const val CAPABILITY_WAKE_DEVICE = "custom.actions.intent.WAKE_DEVICE"
        private const val SHORTCUT_PARAMETER_DEVICE_NAME = "deviceName"
    }
}
