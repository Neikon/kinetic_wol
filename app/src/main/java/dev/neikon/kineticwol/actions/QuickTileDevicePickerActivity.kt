package dev.neikon.kineticwol.actions

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dev.neikon.kineticwol.KineticWolApplication
import dev.neikon.kineticwol.R
import dev.neikon.kineticwol.domain.model.WakeDevice
import dev.neikon.kineticwol.ui.theme.KineticWolTheme
import kotlinx.coroutines.launch

class QuickTileDevicePickerActivity : ComponentActivity() {
    private val appContainer
        get() = (application as KineticWolApplication).appContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KineticWolTheme {
                val devices by appContainer.deviceRepository.observeDevices()
                    .collectAsStateWithLifecycle(initialValue = emptyList())

                QuickTileDevicePickerScreen(
                    devices = devices,
                    onDismiss = ::finish,
                    onWakeDevice = ::wakeDevice,
                )
            }
        }
    }

    private fun wakeDevice(device: WakeDevice) {
        lifecycleScope.launch {
            runCatching {
                appContainer.wakeOnLanSender.send(device)
            }.onSuccess {
                appContainer.deviceShortcutPublisher.reportUsed(device.id)
                Toast.makeText(
                    this@QuickTileDevicePickerActivity,
                    getString(R.string.manual_wake_success, device.name),
                    Toast.LENGTH_SHORT,
                ).show()
            }.onFailure {
                Toast.makeText(
                    this@QuickTileDevicePickerActivity,
                    getString(R.string.manual_wake_error, device.name),
                    Toast.LENGTH_SHORT,
                ).show()
            }

            finish()
        }
    }
}

@Composable
private fun QuickTileDevicePickerScreen(
    devices: List<WakeDevice>,
    onDismiss: () -> Unit,
    onWakeDevice: (WakeDevice) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.36f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = stringResource(id = R.string.quick_tile_picker_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(id = R.string.quick_tile_picker_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(id = R.string.cancel))
                    }
                }

                if (devices.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.quick_tile_picker_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(items = devices, key = { device -> device.id }) { device ->
                            QuickTileDeviceRow(
                                device = device,
                                onWakeDevice = { onWakeDevice(device) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTileDeviceRow(
    device: WakeDevice,
    onWakeDevice: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onWakeDevice,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "${device.host}:${device.port}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(id = R.string.quick_tile_picker_action),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
