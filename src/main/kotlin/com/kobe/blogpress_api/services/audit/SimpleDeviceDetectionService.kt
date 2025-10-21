package com.kobe.hrs.services.audit

import com.kobe.blogpress_api.model.audit.DeviceInfo
import org.springframework.stereotype.Service

@Service
class SimpleDeviceDetectionService : DeviceDetectionService {
    override fun detectDevice(userAgent: String): DeviceInfo? {
        return try {
            DeviceInfo(
                deviceType = detectDeviceType(userAgent),
                operatingSystem = detectOS(userAgent),
                browser = detectBrowser(userAgent),
                browserVersion = detectBrowserVersion(userAgent)
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun detectDeviceType(userAgent: String): String {
        return when {
            userAgent.contains("Mobile", true) -> "mobile"
            userAgent.contains("Tablet", true) -> "tablet"
            else -> "desktop"
        }
    }

    private fun detectOS(userAgent: String): String {
        return when {
            userAgent.contains("Windows", true) -> "Windows"
            userAgent.contains("Mac OS", true) -> "macOS"
            userAgent.contains("Linux", true) -> "Linux"
            userAgent.contains("Android", true) -> "Android"
            userAgent.contains("iPhone", true) -> "iOS"
            else -> "Unknown"
        }
    }

    private fun detectBrowser(userAgent: String): String {
        return when {
            userAgent.contains("Chrome", true) -> "Chrome"
            userAgent.contains("Firefox", true) -> "Firefox"
            userAgent.contains("Safari", true) -> "Safari"
            userAgent.contains("Edge", true) -> "Edge"
            else -> "Unknown"
        }
    }

    private fun detectBrowserVersion(userAgent: String): String {
        // Logique simplifiée pour extraire la version
        val patterns = mapOf(
            "Chrome" to Regex("Chrome/([\\d.]+)"),
            "Firefox" to Regex("Firefox/([\\d.]+)"),
            "Safari" to Regex("Version/([\\d.]+).*Safari"),
            "Edge" to Regex("Edge/([\\d.]+)")
        )

        patterns.forEach { (browser, pattern) ->
            if (userAgent.contains(browser, true)) {
                pattern.find(userAgent)?.let { match ->
                    return match.groupValues[1]
                }
            }
        }

        return "Unknown"
    }
}