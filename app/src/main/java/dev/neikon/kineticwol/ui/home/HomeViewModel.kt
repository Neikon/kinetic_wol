package dev.neikon.kineticwol.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.neikon.kineticwol.AppContainer
import dev.neikon.kineticwol.R
import dev.neikon.kineticwol.domain.model.WakeDevice
import dev.neikon.kineticwol.domain.repository.DeviceRepository
import dev.neikon.kineticwol.domain.wol.WakeOnLanSender
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
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<UiMessage>()
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.observeDevices().collect { devices ->
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
        _uiState.update { it.copy(editor = draft) }
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
                log(UiMessage(R.string.manual_wake_success, listOf(device.name)))
                _messages.emit(UiMessage(R.string.manual_wake_success, listOf(device.name)))
            }.onFailure {
                log(UiMessage(R.string.manual_wake_error, listOf(device.name)))
                _messages.emit(UiMessage(R.string.manual_wake_error, listOf(device.name)))
            }
        }
    }

    private fun validateDraft(draft: DeviceDraft): Map<String, Int> {
        val errors = mutableMapOf<String, Int>()

        if (draft.name.isBlank()) {
            errors["name"] = R.string.validation_name
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

        return errors
    }

    private suspend fun log(message: UiMessage) {
        val timestamp = LocalTime.now().format(TIME_FORMATTER)
        _uiState.update { state ->
            state.copy(
                logs = listOf(EventLog(timestamp = timestamp, message = message)) + state.logs,
            )
        }
    }

    companion object {
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")

        fun factory(appContainer: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HomeViewModel(
                        repository = appContainer.deviceRepository,
                        wakeOnLanSender = appContainer.wakeOnLanSender,
                    ) as T
            }
    }
}
