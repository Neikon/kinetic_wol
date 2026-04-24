package dev.neikon.kineticwol.domain.model

enum class RemoteShutdownMethod {
    AGENT,
    SSH,
}

data class AgentShutdownConfig(
    val baseUrl: String,
    val authToken: String,
)

data class SshShutdownConfig(
    val host: String,
    val port: Int = 22,
    val username: String,
    val privateKey: String,
    val publicKey: String = "",
    val hostKeyFingerprint: String,
    val keyPassphrase: String? = null,
    val command: String = DEFAULT_COMMAND,
) {
    companion object {
        const val DEFAULT_COMMAND = "sudo -n systemctl poweroff"
    }
}

data class RemoteShutdownConfig(
    val enabled: Boolean = false,
    val method: RemoteShutdownMethod = RemoteShutdownMethod.AGENT,
    val agent: AgentShutdownConfig? = null,
    val ssh: SshShutdownConfig? = null,
) {
    val isReady: Boolean
        get() = when {
            !enabled -> false
            method == RemoteShutdownMethod.AGENT -> agent != null
            method == RemoteShutdownMethod.SSH -> ssh != null
            else -> false
        }
}
