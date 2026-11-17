package com.example.network

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

data class DetectedIpResult(
    val primaryIp: String?,
    val allLocalIps: List<String>,
)

object IpAddressProvider {

    fun detectLocalIpv4Addresses(): DetectedIpResult {
        val candidates = mutableListOf<Pair<Int, String>>()
        runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return DetectedIpResult(null, emptyList())
            for (iface in Collections.list(interfaces)) {
                if (!iface.isUp || iface.isLoopback || iface.isVirtual) continue
                val ifaceName = iface.name.lowercase()
                // Skip cellular rmnet/ccmni interfaces when hotspot/wifi interfaces exist
                val baseScore = when {
                    ifaceName.startsWith("ap") || ifaceName.startsWith("swlan") || ifaceName.startsWith("softap") -> 100
                    ifaceName.startsWith("wlan") -> 80
                    ifaceName.startsWith("eth") -> 60
                    ifaceName.startsWith("rmnet") || ifaceName.startsWith("ccmni") || ifaceName.startsWith("tun") -> 5
                    else -> 40
                }
                for (addr in Collections.list(iface.inetAddresses)) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                        val hostAddress = addr.hostAddress ?: continue
                        val ipBonus = when {
                            hostAddress.startsWith("192.168.43.") || hostAddress.startsWith("192.168.49.") -> 30
                            hostAddress.startsWith("192.168.") -> 20
                            addr.isSiteLocalAddress -> 10
                            else -> 0
                        }
                        candidates.add((baseScore + ipBonus) to hostAddress)
                    }
                }
            }
        }
        val sorted = candidates
            .sortedByDescending { it.first }
            .map { it.second }
            .distinct()
        return DetectedIpResult(
            primaryIp = sorted.firstOrNull(),
            allLocalIps = sorted,
        )
    }
}
