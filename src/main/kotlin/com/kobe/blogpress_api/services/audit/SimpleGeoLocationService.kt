package com.kobe.hrs.services.audit

import com.kobe.blogpress_api.model.audit.GeoLocation
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

// Implémentation simple du service de géolocalisation
@Service
class SimpleGeoLocationService : GeoLocationService {
    private val logger = LoggerFactory.getLogger(SimpleGeoLocationService::class.java)

    override fun getLocation(ipAddress: String): GeoLocation? {
        return try {
            // Ici vous pourriez intégrer une API comme MaxMind GeoIP2
            // Pour l'exemple, on retourne des données fictives pour les IPs locales
            if (isLocalIpAddress(ipAddress)) {
                GeoLocation(
                    country = "Cameroun",
                    region = "Centre",
                    city = "Yaoundé",
                    timezone = "Africa/Douala"
                )
            } else {
                // Pour les vraies IPs, vous intégreriez un service de géolocalisation
                null
            }
        } catch (e: Exception) {
            logger.warn("Erreur lors de la géolocalisation de l'IP $ipAddress", e)
            null
        }
    }

    private fun isLocalIpAddress(ip: String): Boolean {
        return ip.startsWith("192.168.") ||
                ip.startsWith("10.") ||
                ip.startsWith("172.") ||
                ip == "127.0.0.1" ||
                ip == "localhost"
    }
}
