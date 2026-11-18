package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InterfaceAddress
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
                if (ifaceName.startsWith("rmnet") ||
                    ifaceName.startsWith("ccmni") ||
                    ifaceName.startsWith("tun") ||
                    ifaceName.startsWith("ppp") ||
                    ifaceName.startsWith("dummy")
                ) {
                    continue
                }

                val baseScore = when {
                    ifaceName.startsWith("ap") ||
                        ifaceName.startsWith("swlan") ||
                        ifaceName.startsWith("softap") ||
                        ifaceName.contains("ap") ||
                        ifaceName.startsWith("rndis") -> 120
                    ifaceName == "wlan1" || ifaceName == "wlan2" -> 105
                    ifaceName.startsWith("wlan") -> 85
                    ifaceName.startsWith("p2p") -> 75
                    ifaceName.startsWith("eth") -> 60
                    else -> 40
                }

                for (addr in Collections.list(iface.inetAddresses)) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                        val hostAddress = addr.hostAddress ?: continue
                        // Skip carrier-grade NAT 100.64.0.0/10
                        if (isCarrierGradeNat(addr)) continue

                        val ipBonus = when {
                            hostAddress == "192.168.43.1" || hostAddress == "192.168.49.1" -> 45
                            hostAddress.startsWith("192.168.") && hostAddress.endsWith(".1") -> 35
                            hostAddress.startsWith("192.168.") -> 20
                            addr.isSiteLocalAddress && hostAddress.endsWith(".1") -> 18
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

    fun findWifiNetwork(context: Context): Network? {
        return runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
            val networks = cm.allNetworks
            networks.firstOrNull { network ->
                val caps = cm.getNetworkCapabilities(network) ?: return@firstOrNull false
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                    !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            }
        }.getOrNull()
    }

    fun detectWifiGatewayIp(context: Context): String? {
        return runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val wifiNet = if (cm != null) findWifiNetwork(context) else null
            if (cm != null && wifiNet != null) {
                val linkProps = cm.getLinkProperties(wifiNet)
                val gw = linkProps?.routes
                    ?.firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }
                    ?.gateway
                    ?.hostAddress
                if (!gw.isNullOrBlank() && gw != "0.0.0.0") {
                    return gw
                }
            }
            @Suppress("DEPRECATION")
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            @Suppress("DEPRECATION")
            val dhcpGateway = wifiManager?.dhcpInfo?.gateway ?: 0
            if (dhcpGateway != 0) {
                val bytes = byteArrayOf(
                    (dhcpGateway and 0xff).toByte(),
                    ((dhcpGateway shr 8) and 0xff).toByte(),
                    ((dhcpGateway shr 16) and 0xff).toByte(),
                    ((dhcpGateway shr 24) and 0xff).toByte(),
                )
                val addr = InetAddress.getByAddress(bytes).hostAddress
                if (!addr.isNullOrBlank() && addr != "0.0.0.0") return addr
            }
            null
        }.getOrNull()
    }

    fun detectBroadcastAddresses(): List<String> {
        val result = linkedSetOf("255.255.255.255")
        runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return result.toList()
            for (iface in Collections.list(interfaces)) {
                if (!iface.isUp || iface.isLoopback || iface.isVirtual) continue
                val ifaceName = iface.name.lowercase()
                if (ifaceName.startsWith("rmnet") || ifaceName.startsWith("ccmni") || ifaceName.startsWith("tun")) {
                    continue
                }
                for (ifaceAddr: InterfaceAddress in iface.interfaceAddresses) {
                    val bcast = ifaceAddr.broadcast
                    if (bcast is Inet4Address && !bcast.isLoopbackAddress) {
                        bcast.hostAddress?.let { result.add(it) }
                    }
                }
            }
        }
        return result.toList()
    }

    private fun isCarrierGradeNat(addr: Inet4Address): Boolean {
        val b = addr.address
        if (b.size != 4) return false
        val first = b[0].toInt() and 0xFF
        val second = b[1].toInt() and 0xFF
        return first == 100 && second in 64..127
    }
}
