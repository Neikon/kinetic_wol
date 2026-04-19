package dev.neikon.kineticwol.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.neikon.kineticwol.AppContainer
import dev.neikon.kineticwol.R
import dev.neikon.kineticwol.actions.DeviceShortcutPublisher
import dev.neikon.kineticwol.domain.model.AgentShutdownConfig
import dev.neikon.kineticwol.domain.model.RemoteShutdownConfig
import dev.neikon.kineticwol.domain.model.RemoteShutdownMethod
import dev.neikon.kineticwol.domain.model.SshShutdownConfig
import dev.neikon.kineticwol.domain.model.WakeDevice
import dev.neikon.kineticwol.domain.repository.DeviceRepository
import dev.neikon.kineticwol.domain.shutdown.AgentPowerOffResult
import dev.neikon.kineticwol.domain.shutdown.AgentRequestFailure
import dev.neikon.kineticwol.domain.shutdown.AgentStatusResult
import dev.neikon.kineticwol.domain.shutdown.AgentShutdownSender
import dev.neikon.kineticwol.domain.shutdown.SshPowerOffResult
import dev.neikon.kineticwol.domain.shutdown.SshKeyMaterialGenerator
import dev.neikon.kineticwol.domain.shutdown.SshRequestFailure
import dev.neikon.kineticwol.domain.shutdown.SshShutdownSender
import dev.neikon.kineticwol.domain.shutdown.SshStatusResult
import dev.neikon.kineticwol.domain.wol.WakeOnLanSender
import dev.neikon.kineticwol.util.normalizeDeviceName
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: DeviceRepository,
    private val wakeOnLanSender: WakeOnLanSender,
    private val agentShutdownSender: AgentShutdownSender,
    private val sshShutdownSender: SshShutdownSender,
    private val sshKeyMaterialGenerator: SshKeyMaterialGenerator,
    private val deviceShortcutPublisher: DeviceShortcutPublisher,
    private val homeScreenPreferences: HomeScreenPreferences,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        HomeUiState(
            isHeroCardVisible = !homeScreenPreferences.isHeroCardDismissed(),
        ),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<UiMessage>()
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.observeDevices().collect { devices ->
                deviceShortcutPublisher.sync(devices)
                _uiState.update { state -> state.copy(devices = devices) }
            }
        }
    }

    fun openCreateDevice() {
        _uiState.update {
            it.copy(
                editor = DeviceDraft(),
                validationErrors = emptyMap(),
            )
        }
    }

    fun openEditDevice(device: WakeDevice) {
        _uiState.update {
            it.copy(
                editor = DeviceDraft(
                    id = device.id,
                    name = device.name,
                    macAddress = device.macAddress,
                    host = device.host,
                    port = device.port.toString(),
                    shutdownEnabled = device.remoteShutdown.enabled,
                    shutdownMethod = device.remoteShutdown.method,
                    agentBaseUrl = device.remoteShutdown.agent?.baseUrl.orEmpty(),
                    agentAuthToken = device.remoteShutdown.agent?.authToken.orEmpty(),
                    sshHost = device.remoteShutdown.ssh?.host.orEmpty(),
                    sshPort = device.remoteShutdown.ssh?.port?.toString().orEmpty().ifBlank { "22" },
                    sshUsername = device.remoteShutdown.ssh?.username.orEmpty(),
                    sshPrivateKey = device.remoteShutdown.ssh?.privateKey.orEmpty(),
                    sshPublicKey = device.remoteShutdown.ssh?.publicKey.orEmpty(),
                    sshKeyPassphrase = device.remoteShutdown.ssh?.keyPassphrase.orEmpty(),
                    sshHostKeyFingerprint = device.remoteShutdown.ssh?.hostKeyFingerprint.orEmpty(),
                    sshCommand = device.remoteShutdown.ssh?.command ?: SshShutdownConfig.DEFAULT_COMMAND,
                ),
                validationErrors = emptyMap(),
            )
        }
    }

    fun dismissEditor() {
        _uiState.update {
            it.copy(
                editor = null,
                validationErrors = emptyMap(),
            )
        }
    }

    fun updateDraft(draft: DeviceDraft) {
        _uiState.update { state ->
            state.copy(
                editor = draft,
                validationErrors = state.validationErrors.filterKeys { key ->
                    key !in changedFields(state.editor, draft)
                },
            )
        }
    }

    fun saveDraft() {
        val draft = uiState.value.editor ?: return
        val validationErrors = validateDraft(draft)

        if (validationErrors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = validationErrors) }
            return
        }

        val device = WakeDevice(
            id = draft.id ?: UUID.randomUUID().toString(),
            name = draft.name.trim(),
            macAddress = draft.macAddress.trim(),
            host = draft.host.trim(),
            port = draft.port.toInt(),
            remoteShutdown =
                RemoteShutdownConfig(
                    enabled = draft.shutdownEnabled,
                    method = draft.shutdownMethod,
                    agent =
                        if (draft.shutdownEnabled && draft.shutdownMethod == RemoteShutdownMethod.AGENT) {
                            AgentShutdownConfig(
                                baseUrl = draft.agentBaseUrl.trim(),
                                authToken = draft.agentAuthToken.trim(),
                            )
                        } else {
                            null
                        },
                    ssh =
                        if (draft.shutdownEnabled && draft.shutdownMethod == RemoteShutdownMethod.SSH) {
                            SshShutdownConfig(
                                host = draft.sshHost.trim(),
                                port = draft.sshPort.toInt(),
                                username = draft.sshUsername.trim(),
                                privateKey = draft.sshPrivateKey,
                                publicKey = draft.sshPublicKey,
                                hostKeyFingerprint = draft.sshHostKeyFingerprint.trim(),
                                keyPassphrase = draft.sshKeyPassphrase.trim().takeIf { it.isNotEmpty() },
                                command = draft.sshCommand.trim().ifBlank { SshShutdownConfig.DEFAULT_COMMAND },
                            )
                        } else {
                            null
                        },
                ),
        )

        viewModelScope.launch {
            repository.upsert(device)
            log(UiMessage(R.string.log_saved_device, listOf(device.name)))
            _messages.emit(UiMessage(R.string.save_success))
            dismissEditor()
        }
    }

    fun deleteCurrentDevice() {
        val draft = uiState.value.editor ?: return
        val deviceId = draft.id ?: return

        viewModelScope.launch {
            repository.delete(deviceId)
            deviceShortcutPublisher.remove(deviceId)
            log(UiMessage(R.string.log_deleted_device, listOf(draft.name)))
            _messages.emit(UiMessage(R.string.delete_success))
            dismissEditor()
        }
    }

    fun wakeDevice(device: WakeDevice) {
        viewModelScope.launch {
            runCatching {
                wakeOnLanSender.send(device)
            }.onSuccess {
                deviceShortcutPublisher.reportUsed(device.id)
                log(UiMessage(R.string.manual_wake_success, listOf(device.name)))
                _messages.emit(UiMessage(R.string.manual_wake_success, listOf(device.name)))
            }.onFailure {
                log(UiMessage(R.string.manual_wake_error, listOf(device.name)))
                _messages.emit(UiMessage(R.string.manual_wake_error, listOf(device.name)))
            }
        }
    }

    fun shutdownDevice(device: WakeDevice) {
        viewModelScope.launch {
            when (device.remoteShutdown.method) {
                RemoteShutdownMethod.AGENT -> {
                    when (val result = agentShutdownSender.send(device)) {
                        is AgentPowerOffResult.Success -> {
                            log(UiMessage(R.string.remote_shutdown_success, listOf(device.name)))
                            _messages.emit(UiMessage(R.string.remote_shutdown_success, listOf(device.name)))
                        }

                        is AgentPowerOffResult.Failure -> {
                            val message = powerOffFailureMessage(device.name, result.error)
                            log(message)
                            _messages.emit(message)
                        }
                    }
                }

                RemoteShutdownMethod.SSH -> {
                    when (val result = sshShutdownSender.send(device)) {
                        is SshPowerOffResult.Success -> {
                            log(UiMessage(R.string.remote_shutdown_success, listOf(device.name)))
                            _messages.emit(UiMessage(R.string.remote_shutdown_success, listOf(device.name)))
                        }

                        is SshPowerOffResult.Failure -> {
                            val message = sshPowerOffFailureMessage(device.name, result.error)
                            log(message)
                            _messages.emit(message)
                        }
                    }
                }
            }
        }
    }

    fun testRemoteShutdownConnection() {
        val draft = uiState.value.editor ?: return
        val validationErrors = validateShutdownConnectionDraft(draft)

        if (validationErrors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    validationErrors = it.validationErrors + validationErrors,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { state -> state.copy(isTestingShutdownConnection = true) }
            val message =
                when (draft.shutdownMethod) {
                    RemoteShutdownMethod.AGENT -> {
                        val result =
                            agentShutdownSender.checkStatus(
                                AgentShutdownConfig(
                                    baseUrl = draft.agentBaseUrl.trim(),
                                    authToken = draft.agentAuthToken.trim(),
                                ),
                            )

                        when (result) {
                            is AgentStatusResult.Success -> {
                                if (result.data.canPowerOff.equals("yes", ignoreCase = true)) {
                                    UiMessage(
                                        R.string.agent_status_ready,
                                        listOf(result.data.message.ifBlank { result.data.canPowerOff }),
                                    )
                                } else {
                                    UiMessage(
                                        R.string.agent_status_not_ready,
                                        listOf(result.data.message.ifBlank { result.data.canPowerOff }),
                                    )
                                }
                            }

                            is AgentStatusResult.Failure -> statusFailureMessage(result.error)
                        }
                    }

                    RemoteShutdownMethod.SSH -> {
                        val result =
                            sshShutdownSender.checkStatus(
                                SshShutdownConfig(
                                    host = draft.sshHost.trim(),
                                    port = draft.sshPort.toInt(),
                                    username = draft.sshUsername.trim(),
                                    privateKey = draft.sshPrivateKey,
                                    publicKey = draft.sshPublicKey,
                                    hostKeyFingerprint = draft.sshHostKeyFingerprint.trim(),
                                    keyPassphrase = draft.sshKeyPassphrase.trim().takeIf { it.isNotEmpty() },
                                    command = draft.sshCommand.trim().ifBlank { SshShutdownConfig.DEFAULT_COMMAND },
                                ),
                            )

                        when (result) {
                            is SshStatusResult.Success -> {
                                if (draft.sshHostKeyFingerprint.isBlank()) {
                                    _uiState.update { state ->
                                        state.copy(
                                            editor = state.editor?.copy(
                                                sshHostKeyFingerprint = result.data.hostKeyFingerprint,
                                            ),
                                            validationErrors =
                                                state.validationErrors - "sshHostKeyFingerprint",
                                        )
                                    }
                                    UiMessage(
                                        R.string.ssh_status_ready_with_fingerprint,
                                        listOf(result.data.hostKeyFingerprint),
                                    )
                                } else {
                                    UiMessage(
                                        R.string.ssh_status_ready,
                                        listOf(result.data.message),
                                    )
                                }
                            }

                            is SshStatusResult.Failure -> sshStatusFailureMessage(result.error)
                        }
                    }
                }

            _uiState.update { state -> state.copy(isTestingShutdownConnection = false) }
            _messages.emit(message)
        }
    }

    fun dismissHeroCard() {
        homeScreenPreferences.setHeroCardDismissed(true)
        _uiState.update { state -> state.copy(isHeroCardVisible = false) }
    }

    fun generateSshKeyPair() {
        val draft = uiState.value.editor ?: return

        viewModelScope.launch {
            val generated = sshKeyMaterialGenerator.generate()
            _uiState.update { state ->
                state.copy(
                    editor = state.editor?.copy(
                        sshPrivateKey = generated.privateKeyPem,
                        sshPublicKey = generated.publicKeyAuthorized,
                        sshKeyPassphrase = "",
                    ),
                    validationErrors = state.validationErrors - setOf("sshPrivateKey"),
                )
            }
            _messages.emit(UiMessage(R.string.ssh_keypair_generated))
        }
    }

    private fun validateDraft(draft: DeviceDraft): Map<String, Int> {
        val errors = mutableMapOf<String, Int>()

        if (draft.name.isBlank()) {
            errors["name"] = R.string.validation_name
        } else {
            val normalizedDraftName = normalizeDeviceName(draft.name)
            val hasDuplicate = uiState.value.devices.any { device ->
                device.id != draft.id && normalizeDeviceName(device.name) == normalizedDraftName
            }
            if (hasDuplicate) {
                errors["name"] = R.string.validation_name_duplicate
            }
        }
        if (draft.host.isBlank()) {
            errors["host"] = R.string.validation_host
        }

        val portValue = draft.port.toIntOrNull()
        if (portValue == null || portValue !in 1..65535) {
            errors["port"] = R.string.validation_port
        }

        runCatching { wakeOnLanSender.normalizeMac(draft.macAddress) }
            .onFailure {
                errors["macAddress"] = R.string.validation_mac
            }

        if (draft.shutdownEnabled) {
            when (draft.shutdownMethod) {
                RemoteShutdownMethod.AGENT -> {
                    if (draft.agentBaseUrl.isBlank()) {
                        errors["agentBaseUrl"] = R.string.validation_agent_base_url
                    }
                    if (draft.agentAuthToken.isBlank()) {
                        errors["agentAuthToken"] = R.string.validation_agent_auth_token
                    }
                }

                RemoteShutdownMethod.SSH -> {
                    if (draft.sshHost.isBlank()) {
                        errors["sshHost"] = R.string.validation_ssh_host
                    }
                    val sshPortValue = draft.sshPort.toIntOrNull()
                    if (sshPortValue == null || sshPortValue !in 1..65535) {
                        errors["sshPort"] = R.string.validation_ssh_port
                    }
                    if (draft.sshUsername.isBlank()) {
                        errors["sshUsername"] = R.string.validation_ssh_username
                    }
                    if (draft.sshPrivateKey.isBlank()) {
                        errors["sshPrivateKey"] = R.string.validation_ssh_private_key
                    }
                    if (draft.sshHostKeyFingerprint.isBlank()) {
                        errors["sshHostKeyFingerprint"] = R.string.validation_ssh_host_key_fingerprint
                    }
                }
            }
        }

        return errors
    }

    private fun validateShutdownConnectionDraft(draft: DeviceDraft): Map<String, Int> {
        val errors = mutableMapOf<String, Int>()
        when (draft.shutdownMethod) {
            RemoteShutdownMethod.AGENT -> {
                if (draft.agentBaseUrl.isBlank()) {
                    errors["agentBaseUrl"] = R.string.validation_agent_base_url
                }
                if (draft.agentAuthToken.isBlank()) {
                    errors["agentAuthToken"] = R.string.validation_agent_auth_token
                }
            }

            RemoteShutdownMethod.SSH -> {
                if (draft.sshHost.isBlank()) {
                    errors["sshHost"] = R.string.validation_ssh_host
                }
                val sshPortValue = draft.sshPort.toIntOrNull()
                if (sshPortValue == null || sshPortValue !in 1..65535) {
                    errors["sshPort"] = R.string.validation_ssh_port
                }
                if (draft.sshUsername.isBlank()) {
                    errors["sshUsername"] = R.string.validation_ssh_username
                }
                if (draft.sshPrivateKey.isBlank()) {
                    errors["sshPrivateKey"] = R.string.validation_ssh_private_key
                }
            }
        }
        return errors
    }

    private suspend fun log(message: UiMessage) {
        val timestamp = LocalTime.now().format(TIME_FORMATTER)
        _uiState.update { state ->
            state.copy(
                logs = (listOf(EventLog(timestamp = timestamp, message = message)) + state.logs)
                    .take(MAX_LOG_ENTRIES),
            )
        }
    }

    private fun changedFields(
        previous: DeviceDraft?,
        current: DeviceDraft,
    ): Set<String> {
        if (previous == null) return emptySet()

        val changed = mutableSetOf<String>()
        if (previous.name != current.name) changed += "name"
        if (previous.macAddress != current.macAddress) changed += "macAddress"
        if (previous.host != current.host) changed += "host"
        if (previous.port != current.port) changed += "port"
        if (previous.shutdownEnabled != current.shutdownEnabled) {
            changed += "shutdownEnabled"
            changed += "agentBaseUrl"
            changed += "agentAuthToken"
            changed += "sshHost"
            changed += "sshPort"
            changed += "sshUsername"
            changed += "sshPrivateKey"
            changed += "sshPublicKey"
            changed += "sshKeyPassphrase"
            changed += "sshHostKeyFingerprint"
            changed += "sshCommand"
        }
        if (previous.shutdownMethod != current.shutdownMethod) changed += "shutdownMethod"
        if (previous.agentBaseUrl != current.agentBaseUrl) changed += "agentBaseUrl"
        if (previous.agentAuthToken != current.agentAuthToken) changed += "agentAuthToken"
        if (previous.sshHost != current.sshHost) changed += "sshHost"
        if (previous.sshPort != current.sshPort) changed += "sshPort"
        if (previous.sshUsername != current.sshUsername) changed += "sshUsername"
        if (previous.sshPrivateKey != current.sshPrivateKey) changed += "sshPrivateKey"
        if (previous.sshPublicKey != current.sshPublicKey) changed += "sshPublicKey"
        if (previous.sshKeyPassphrase != current.sshKeyPassphrase) changed += "sshKeyPassphrase"
        if (previous.sshHostKeyFingerprint != current.sshHostKeyFingerprint) {
            changed += "sshHostKeyFingerprint"
        }
        if (previous.sshCommand != current.sshCommand) changed += "sshCommand"
        return changed
    }

    companion object {
        private const val MAX_LOG_ENTRIES = 50
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")

        fun factory(appContainer: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HomeViewModel(
                        repository = appContainer.deviceRepository,
                        wakeOnLanSender = appContainer.wakeOnLanSender,
                        agentShutdownSender = appContainer.agentShutdownSender,
                        sshShutdownSender = appContainer.sshShutdownSender,
                        sshKeyMaterialGenerator = appContainer.sshKeyMaterialGenerator,
                        deviceShortcutPublisher = appContainer.deviceShortcutPublisher,
                        homeScreenPreferences = appContainer.homeScreenPreferences,
                    ) as T
            }
    }

    private fun statusFailureMessage(error: AgentRequestFailure): UiMessage =
        when (error) {
            is AgentRequestFailure.InvalidBaseUrl -> UiMessage(R.string.agent_status_invalid_base_url)
            is AgentRequestFailure.HostUnreachable -> UiMessage(R.string.agent_status_host_unreachable)
            is AgentRequestFailure.ConnectionRefused ->
                UiMessage(R.string.agent_status_connection_refused)
            is AgentRequestFailure.Unauthorized -> UiMessage(R.string.agent_status_invalid_token)
            is AgentRequestFailure.NotFound -> UiMessage(R.string.agent_status_not_found)
            is AgentRequestFailure.BackendUnavailable ->
                UiMessage(
                    R.string.agent_status_backend_unavailable,
                    listOf(error.message.orEmpty()),
                )
            is AgentRequestFailure.Timeout -> UiMessage(R.string.agent_status_timeout)
            is AgentRequestFailure.CleartextBlocked -> UiMessage(R.string.agent_status_cleartext_blocked)
            is AgentRequestFailure.Ssl -> UiMessage(R.string.agent_status_ssl_error)
            is AgentRequestFailure.Network -> UiMessage(R.string.agent_status_network_error)
            is AgentRequestFailure.Unknown -> UiMessage(R.string.agent_status_unexpected_error)
        }

    private fun powerOffFailureMessage(
        deviceName: String,
        error: AgentRequestFailure,
    ): UiMessage =
        when (error) {
            is AgentRequestFailure.InvalidBaseUrl ->
                UiMessage(R.string.remote_shutdown_invalid_base_url, listOf(deviceName))
            is AgentRequestFailure.HostUnreachable ->
                UiMessage(R.string.remote_shutdown_host_unreachable, listOf(deviceName))
            is AgentRequestFailure.ConnectionRefused ->
                UiMessage(R.string.remote_shutdown_connection_refused, listOf(deviceName))
            is AgentRequestFailure.Unauthorized ->
                UiMessage(R.string.remote_shutdown_invalid_token, listOf(deviceName))
            is AgentRequestFailure.NotFound ->
                UiMessage(R.string.remote_shutdown_not_found, listOf(deviceName))
            is AgentRequestFailure.BackendUnavailable ->
                UiMessage(R.string.remote_shutdown_backend_unavailable)
            is AgentRequestFailure.Timeout ->
                UiMessage(R.string.remote_shutdown_timeout, listOf(deviceName))
            is AgentRequestFailure.CleartextBlocked ->
                UiMessage(R.string.remote_shutdown_cleartext_blocked, listOf(deviceName))
            is AgentRequestFailure.Ssl ->
                UiMessage(R.string.remote_shutdown_ssl_error, listOf(deviceName))
            is AgentRequestFailure.Network ->
                UiMessage(R.string.remote_shutdown_network_error, listOf(deviceName))
            is AgentRequestFailure.Unknown ->
                UiMessage(R.string.remote_shutdown_error, listOf(deviceName))
        }

    private fun sshStatusFailureMessage(error: SshRequestFailure): UiMessage =
        when (error) {
            is SshRequestFailure.InvalidConfiguration ->
                UiMessage(R.string.ssh_status_invalid_configuration)
            is SshRequestFailure.HostUnreachable ->
                UiMessage(R.string.ssh_status_host_unreachable)
            is SshRequestFailure.ConnectionRefused ->
                UiMessage(R.string.ssh_status_connection_refused)
            is SshRequestFailure.AuthenticationFailed ->
                UiMessage(R.string.ssh_status_auth_failed)
            is SshRequestFailure.HostKeyMismatch ->
                UiMessage(R.string.ssh_status_host_key_mismatch)
            is SshRequestFailure.InvalidPrivateKey ->
                UiMessage(R.string.ssh_status_invalid_private_key)
            is SshRequestFailure.CommandFailed ->
                UiMessage(R.string.ssh_status_command_failed)
            is SshRequestFailure.Timeout ->
                UiMessage(R.string.ssh_status_timeout)
            is SshRequestFailure.Network ->
                UiMessage(R.string.ssh_status_network_error)
            is SshRequestFailure.Unknown ->
                UiMessage(R.string.ssh_status_unexpected_error)
        }

    private fun sshPowerOffFailureMessage(
        deviceName: String,
        error: SshRequestFailure,
    ): UiMessage =
        when (error) {
            is SshRequestFailure.InvalidConfiguration ->
                UiMessage(R.string.remote_shutdown_ssh_invalid_configuration, listOf(deviceName))
            is SshRequestFailure.HostUnreachable ->
                UiMessage(R.string.remote_shutdown_ssh_host_unreachable, listOf(deviceName))
            is SshRequestFailure.ConnectionRefused ->
                UiMessage(R.string.remote_shutdown_ssh_connection_refused, listOf(deviceName))
            is SshRequestFailure.AuthenticationFailed ->
                UiMessage(R.string.remote_shutdown_ssh_auth_failed, listOf(deviceName))
            is SshRequestFailure.HostKeyMismatch ->
                UiMessage(R.string.remote_shutdown_ssh_host_key_mismatch, listOf(deviceName))
            is SshRequestFailure.InvalidPrivateKey ->
                UiMessage(R.string.remote_shutdown_ssh_invalid_private_key, listOf(deviceName))
            is SshRequestFailure.CommandFailed ->
                UiMessage(R.string.remote_shutdown_ssh_command_failed, listOf(deviceName))
            is SshRequestFailure.Timeout ->
                UiMessage(R.string.remote_shutdown_ssh_timeout, listOf(deviceName))
            is SshRequestFailure.Network ->
                UiMessage(R.string.remote_shutdown_ssh_network_error, listOf(deviceName))
            is SshRequestFailure.Unknown ->
                UiMessage(R.string.remote_shutdown_error, listOf(deviceName))
        }
}
