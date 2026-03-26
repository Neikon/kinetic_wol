package dev.neikon.kineticwol.domain.discovery

import dev.neikon.kineticwol.domain.model.DiscoveryCandidate
import java.io.File
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class NetworkDeviceDiscovery {
    suspend fun discoverCandidates(): List<DiscoveryCandidate> = withContext(Dispatchers.IO) {
        val gateways = localGatewayAddresses()
        if (gateways.isEmpty()) return@withContext emptyList()

        val arpEntries = readArpTable()

        coroutineScope {
            gateways.flatMap { gateway ->
                hostRange(gateway).map { hostAddress ->
                    async {
                        resolveCandidate(hostAddress, arpEntries[hostAddress])
                    }
                }
            }.awaitAll()
                .filterNotNull()
                .distinctBy { candidate -> candidate.host }
                .sortedWith(
                    compareBy<DiscoveryCandidate> { it.name.lowercase(Locale.ROOT) }
                        .thenBy { it.host },
                )
        }
    }

    private fun localGatewayAddresses(): List<String> =
        NetworkInterface.getNetworkInterfaces()
            ?.let(Collections::list)
            .orEmpty()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual }
            .flatMap { networkInterface -> Collections.list(networkInterface.inetAddresses) }
            .filterIsInstance<Inet4Address>()
            .map { it.hostAddress.orEmpty() }
            .filter { address -> address.isNotBlank() && isPrivateIpv4(address) }
            .distinct()

    private fun hostRange(address: String): List<String> {
        val octets = address.split(".")
        if (octets.size != 4) return emptyList()

        val prefix = octets.take(3).joinToString(".")
        val ownHost = octets[3].toIntOrNull() ?: return emptyList()

        return (1..254)
            .asSequence()
            .filterNot { it == ownHost }
            .map { host -> "$prefix.$host" }
            .toList()
    }

    private fun readArpTable(): Map<String, String> {
        val arpFile = File(ARP_FILE_PATH)
        if (!arpFile.exists() || !arpFile.canRead()) return emptyMap()

        return arpFile.readLines()
            .drop(1)
            .mapNotNull { line ->
                val columns = line.trim().split(WHITESPACE_REGEX)
                val ipAddress = columns.getOrNull(0)
                val macAddress = columns.getOrNull(3)

                if (ipAddress.isNullOrBlank() || macAddress.isNullOrBlank()) {
                    null
                } else if (!isMacAddress(macAddress)) {
                    null
                } else {
                    ipAddress to macAddress.uppercase(Locale.ROOT)
                }
            }
            .toMap()
    }

    private fun resolveCandidate(
        hostAddress: String,
        macAddress: String?,
    ): DiscoveryCandidate? {
        val inetAddress = runCatching { InetAddress.getByName(hostAddress) }.getOrNull() ?: return null
        val reachable = runCatching { inetAddress.isReachable(REACHABILITY_TIMEOUT_MS) }.getOrDefault(false)

        if (!reachable && macAddress == null) return null

        val canonicalName = runCatching { inetAddress.canonicalHostName.orEmpty() }.getOrDefault("")
        val label = canonicalName
            .takeIf { it.isNotBlank() && it != hostAddress }
            ?.substringBefore('.')
            ?.takeIf { it.isNotBlank() }
            ?: hostAddress

        return DiscoveryCandidate(
            name = label,
            host = hostAddress,
            macAddress = macAddress,
        )
    }

    private fun isPrivateIpv4(address: String): Boolean {
        val octets = address.split(".")
        if (octets.size != 4) return false

        val first = octets[0].toIntOrNull() ?: return false
        val second = octets[1].toIntOrNull() ?: return false

        return when {
            first == 10 -> true
            first == 192 && second == 168 -> true
            first == 172 && second in 16..31 -> true
            else -> false
        }
    }

    private fun isMacAddress(value: String): Boolean =
        value.matches(MAC_ADDRESS_REGEX)

    companion object {
        private const val REACHABILITY_TIMEOUT_MS = 120
        private const val ARP_FILE_PATH = "/proc/net/arp"
        private val WHITESPACE_REGEX = Regex("\\s+")
        private val MAC_ADDRESS_REGEX = Regex("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}")
    }
}
