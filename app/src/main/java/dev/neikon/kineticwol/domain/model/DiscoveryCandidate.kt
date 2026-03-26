package dev.neikon.kineticwol.domain.model

data class DiscoveryCandidate(
    val name: String,
    val host: String,
    val macAddress: String?,
)
