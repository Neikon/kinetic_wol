package dev.neikon.kineticwol.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.neikon.kineticwol.AppContainer
import dev.neikon.kineticwol.R
import dev.neikon.kineticwol.domain.model.DiscoveryCandidate
import dev.neikon.kineticwol.domain.model.WakeDevice
import dev.neikon.kineticwol.ui.home.DeviceDraft
import dev.neikon.kineticwol.ui.home.DiscoveryUiState
import dev.neikon.kineticwol.ui.home.EventLog
import dev.neikon.kineticwol.ui.home.HomeUiState
import dev.neikon.kineticwol.ui.home.HomeViewModel

@Composable
fun KineticWolApp(
    appContainer: AppContainer,
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(appContainer))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    LaunchedEffect(viewModel, context, configuration) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(
                context.resources.getString(message.resId, *message.formatArgs.toTypedArray()),
            )
        }
    }

    KineticWolScaffold(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAddDevice = viewModel::openCreateDevice,
        onEditDevice = viewModel::openEditDevice,
        onDismissEditor = viewModel::dismissEditor,
        onDraftChange = viewModel::updateDraft,
        onScanDiscoveryCandidates = viewModel::scanDiscoveryCandidates,
        onApplyDiscoveryCandidate = viewModel::applyDiscoveryCandidate,
        onSaveDraft = viewModel::saveDraft,
        onDeleteDraft = viewModel::deleteCurrentDevice,
        onWakeDevice = viewModel::wakeDevice,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KineticWolScaffold(
    uiState: HomeUiState,
    snackbarHostState: SnackbarHostState,
    onAddDevice: () -> Unit,
    onEditDevice: (WakeDevice) -> Unit,
    onDismissEditor: () -> Unit,
    onDraftChange: (DeviceDraft) -> Unit,
    onScanDiscoveryCandidates: () -> Unit,
    onApplyDiscoveryCandidate: (DiscoveryCandidate) -> Unit,
    onSaveDraft: () -> Unit,
    onDeleteDraft: () -> Unit,
    onWakeDevice: (WakeDevice) -> Unit,
) {
    BackHandler(enabled = uiState.editor != null) {
        onDismissEditor()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
        },
        floatingActionButton = {
            if (uiState.editor == null) {
                FloatingActionButton(onClick = onAddDevice) {
                    Text(text = "+")
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Crossfade(
            targetState = uiState.editor != null,
            modifier = Modifier.fillMaxSize(),
            label = "kinetic-wol-screen",
        ) { isEditing ->
            if (!isEditing) {
                DashboardContent(
                    uiState = uiState,
                    onAddDevice = onAddDevice,
                    onEditDevice = onEditDevice,
                    onWakeDevice = onWakeDevice,
                    modifier = Modifier.padding(innerPadding),
                )
            } else {
                val editor = uiState.editor ?: return@Crossfade
                DeviceEditorContent(
                    draft = editor,
                    validationErrors = uiState.validationErrors,
                    discoveryUiState = uiState.discoveryUiState,
                    onDismiss = onDismissEditor,
                    onDraftChange = onDraftChange,
                    onScanDiscoveryCandidates = onScanDiscoveryCandidates,
                    onApplyDiscoveryCandidate = onApplyDiscoveryCandidate,
                    onSave = onSaveDraft,
                    onDelete = onDeleteDraft,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    uiState: HomeUiState,
    onAddDevice: () -> Unit,
    onEditDevice: (WakeDevice) -> Unit,
    onWakeDevice: (WakeDevice) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HeroCard(
                deviceCount = uiState.devices.size,
                logCount = uiState.logs.size,
            )
        }

        item {
            VoiceCard()
        }

        if (uiState.devices.isEmpty()) {
            item {
                EmptyDevicesCard(onAddDevice = onAddDevice)
            }
        } else {
            items(
                items = uiState.devices,
                key = { device -> device.id },
            ) { device ->
                DeviceCard(
                    device = device,
                    onEdit = { onEditDevice(device) },
                    onWake = { onWakeDevice(device) },
                )
            }
        }

        item {
            LogsCard(logs = uiState.logs)
        }
    }
}

@Composable
private fun HeroCard(
    deviceCount: Int,
    logCount: Int,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    ),
                )
                .padding(24.dp),
        ) {
            Text(
                text = stringResource(id = R.string.hero_overline),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(id = R.string.hero_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.hero_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryPill(text = stringResource(id = R.string.summary_devices, deviceCount))
                SummaryPill(text = stringResource(id = R.string.summary_logs, logCount))
            }
        }
    }
}

@Composable
private fun SummaryPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun VoiceCard() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(id = R.string.voice_section_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(id = R.string.voice_section_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(id = R.string.voice_example),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
            Text(
                text = stringResource(id = R.string.app_actions_limit_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyDevicesCard(
    onAddDevice: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(id = R.string.empty_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(id = R.string.empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onAddDevice) {
                Text(text = stringResource(id = R.string.add_device))
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: WakeDevice,
    onEdit: () -> Unit,
    onWake: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${device.host}:${device.port}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "WOL",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }

            Text(
                text = device.macAddress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(id = R.string.edit_device))
                }
                Button(
                    onClick = onWake,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(id = R.string.wake))
                }
            }
        }
    }
}

@Composable
private fun LogsCard(logs: List<EventLog>) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(id = R.string.logs_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (logs.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.logs_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                logs.forEach { entry ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = entry.timestamp,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(
                                id = entry.message.resId,
                                *entry.message.formatArgs.toTypedArray(),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceEditorContent(
    draft: DeviceDraft,
    validationErrors: Map<String, Int>,
    discoveryUiState: DiscoveryUiState,
    onDismiss: () -> Unit,
    onDraftChange: (DeviceDraft) -> Unit,
    onScanDiscoveryCandidates: () -> Unit,
    onApplyDiscoveryCandidate: (DiscoveryCandidate) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        id = if (draft.id == null) R.string.editor_title_new else R.string.editor_title_edit,
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(id = R.string.editor_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(start = 12.dp),
            ) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DraftTextField(
                    label = stringResource(id = R.string.name_label),
                    value = draft.name,
                    error = validationErrors["name"],
                    placeholder = stringResource(id = R.string.form_hint_name),
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                    onValueChange = { onDraftChange(draft.copy(name = it)) },
                )
                DraftTextField(
                    label = stringResource(id = R.string.mac_label),
                    value = draft.macAddress,
                    error = validationErrors["macAddress"],
                    placeholder = stringResource(id = R.string.form_hint_mac),
                    keyboardType = KeyboardType.Ascii,
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next,
                    onValueChange = { onDraftChange(draft.copy(macAddress = it)) },
                )
                DraftTextField(
                    label = stringResource(id = R.string.host_label),
                    value = draft.host,
                    error = validationErrors["host"],
                    placeholder = stringResource(id = R.string.form_hint_host),
                    keyboardType = KeyboardType.Uri,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Next,
                    onValueChange = { onDraftChange(draft.copy(host = it)) },
                )
                DraftTextField(
                    label = stringResource(id = R.string.port_label),
                    value = draft.port,
                    error = validationErrors["port"],
                    placeholder = stringResource(id = R.string.form_hint_port),
                    keyboardType = KeyboardType.Number,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Done,
                    onValueChange = { onDraftChange(draft.copy(port = it)) },
                )
            }
        }

        DiscoveryCandidatesCard(
            discoveryUiState = discoveryUiState,
            onScan = onScanDiscoveryCandidates,
            onApplyCandidate = onApplyDiscoveryCandidate,
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Text(
                text = stringResource(id = R.string.editor_help),
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(id = R.string.save))
        }

        if (draft.id != null) {
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = R.string.delete))
            }
        }
    }
}

@Composable
private fun DiscoveryCandidatesCard(
    discoveryUiState: DiscoveryUiState,
    onScan: () -> Unit,
    onApplyCandidate: (DiscoveryCandidate) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.discovery_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(id = R.string.discovery_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = onScan, enabled = !discoveryUiState.isScanning) {
                    if (discoveryUiState.isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .width(18.dp)
                                .height(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(text = stringResource(id = R.string.discovery_scan))
                    }
                }
            }

            when {
                discoveryUiState.candidates.isNotEmpty() -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        discoveryUiState.candidates.forEach { candidate ->
                            DiscoveryCandidateRow(
                                candidate = candidate,
                                onClick = { onApplyCandidate(candidate) },
                            )
                        }
                    }
                }

                discoveryUiState.isScanning -> {
                    Text(
                        text = stringResource(id = R.string.discovery_scanning),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                discoveryUiState.hasSearched -> {
                    Text(
                        text = stringResource(id = R.string.discovery_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    Text(
                        text = stringResource(id = R.string.discovery_idle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoveryCandidateRow(
    candidate: DiscoveryCandidate,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = candidate.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = candidate.host,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = candidate.macAddress?.let { mac ->
                    stringResource(id = R.string.discovery_mac_detected, mac)
                } ?: stringResource(id = R.string.discovery_mac_missing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun DraftTextField(
    label: String,
    value: String,
    error: Int?,
    placeholder: String,
    keyboardType: KeyboardType,
    capitalization: KeyboardCapitalization,
    imeAction: ImeAction,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(text = label) },
            placeholder = { Text(text = placeholder) },
            isError = error != null,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = keyboardType,
                capitalization = capitalization,
                imeAction = imeAction,
            ),
        )
        if (error != null) {
            Text(
                text = stringResource(id = error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
