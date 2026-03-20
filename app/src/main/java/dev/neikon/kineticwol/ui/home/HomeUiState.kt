package dev.neikon.kineticwol.ui.home

import androidx.annotation.StringRes
import dev.neikon.kineticwol.domain.model.WakeDevice

data class HomeUiState(
    val devices: List<WakeDevice> = emptyList(),
    val logs: List<EventLog> = emptyList(),
    val editor: DeviceDraft? = null,
    val validationErrors: Map<String, Int> = emptyMap(),
)

data class EventLog(
    val timestamp: String,
    val message: UiMessage,
)

data class UiMessage(
    @param:StringRes val resId: Int,
    val formatArgs: List<Any> = emptyList(),
)

data class DeviceDraft(
    val id: String? = null,
    val name: String = "",
    val macAddress: String = "",
    val host: String = "255.255.255.255",
    val port: String = "9",
)
