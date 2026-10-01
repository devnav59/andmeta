package com.example.network

import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {

    data class DeviceIpInfo(
        val ip: String,
        val interfaceName: String,
        val label: String
    )

    fun getDeviceIpAddresses(): List<DeviceIpInfo> {
        val list = mutableListOf<DeviceIpInfo>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return list
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (!iface.isUp) continue

                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet4Address) {
                        val ip = addr.hostAddress ?: continue
                        val name = iface.name.lowercase()
                        val label = when {
                            addr.isLoopbackAddress -> "Localhost (Loopback)"
                            name.contains("wlan") -> "Wi-Fi LAN"
                            name.contains("ap") || name.contains("rndis") -> "Hotspot / Tethering"
                            name.contains("rmnet") || name.contains("ccmni") -> "Cellular / Mobile Data"
                            else -> "Interface ($name)"
                        }
                        list.add(DeviceIpInfo(ip, iface.name, label))
                    }
                }
            }
        } catch (_: Exception) {}

        // Ensure 127.0.0.1 and 10.0.2.2 are available in the list
        if (list.none { it.ip == "127.0.0.1" }) {
            list.add(0, DeviceIpInfo("127.0.0.1", "lo", "Localhost (Winlator)"))
        }
        if (list.none { it.ip == "10.0.2.2" }) {
            list.add(DeviceIpInfo("10.0.2.2", "nat", "Winlator Container Gateway"))
        }

        return list
    }

    fun getPrimaryRecommendedIp(): String {
        val all = getDeviceIpAddresses()
        // First preference: Wi-Fi IP (reaches across containers and local network)
        val wifi = all.firstOrNull { it.label.contains("Wi-Fi") }
        if (wifi != null) return wifi.ip

        // Second preference: 127.0.0.1
        return "127.0.0.1"
    }
}
