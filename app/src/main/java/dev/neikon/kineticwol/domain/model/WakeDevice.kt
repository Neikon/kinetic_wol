package dev.neikon.kineticwol.domain.model

data class WakeDevice(
    val id: String,
    val name: String,
    val macAddress: String,
    val host: String,
    val port: Int = 9,
)
