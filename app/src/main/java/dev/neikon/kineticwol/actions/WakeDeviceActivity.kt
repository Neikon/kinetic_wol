package dev.neikon.kineticwol.actions

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dev.neikon.kineticwol.KineticWolApplication
import kotlinx.coroutines.launch

class WakeDeviceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appContainer = (application as KineticWolApplication).appContainer

        lifecycleScope.launch {
            val deviceName = extractDeviceName()

            if (deviceName.isNullOrBlank()) {
                Log.w(TAG, "Missing device name in wake intent")
                setResult(Activity.RESULT_CANCELED)
                finish()
                return@launch
            }

            runCatching {
                val device = appContainer.deviceRepository.findByName(deviceName)
                if (device == null) {
                    Log.w(TAG, "Requested device not found: $deviceName")
                    setResult(Activity.RESULT_CANCELED)
                } else {
                    Log.i(TAG, "Sending WOL packet for device: ${device.name}")
                    appContainer.wakeOnLanSender.send(device)
                    appContainer.deviceShortcutPublisher.reportUsed(device.id)
                    setResult(Activity.RESULT_OK)
                }
            }.onFailure { throwable ->
                Log.e(TAG, "Failed to send WOL packet", throwable)
                setResult(Activity.RESULT_CANCELED)
            }

            finish()
        }
    }

    private fun extractDeviceName(): String? {
        val extras = intent.extras ?: return null
        val knownKeys = listOf("deviceName", "device.name", "name")

        knownKeys.forEach { key ->
            extras.getString(key)?.takeIf { it.isNotBlank() }?.let { return it }
        }

        return extras.keySet()
            .firstNotNullOfOrNull { key -> extras.getString(key)?.takeIf { it.isNotBlank() } }
    }

    companion object {
        private const val TAG = "WakeDeviceActivity"
    }
}
