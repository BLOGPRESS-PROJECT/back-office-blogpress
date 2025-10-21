package com.kobe.blogpress_api.model.audit

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("audit_logs")
data class AuditLog(
    @Id val id: ObjectId = ObjectId(),
    val userId: ObjectId,
    val action: String,
    val resource: String,
    val resourceId: String? = null,
    val details: Map<String, Any> = emptyMap(),
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val description: String? = null,
    val timestamp: Instant = Instant.now(),
    val success: Boolean = true,

    // Nouveaux champs pour la non-répudiation
    val sessionId: String? = null,
    val requestId: String? = null,
    val httpMethod: String? = null,
    val endpoint: String? = null,
    val requestHeaders: Map<String, String> = emptyMap(),
    val requestParameters: Map<String, Any> = emptyMap(),
    val requestBody: String? = null,
    val responseStatus: Int? = null,
    val responseBody: String? = null,
    val executionTimeMs: Long? = null,
    val errorMessage: String? = null,
    val stackTrace: String? = null,

    // Données de géolocalisation (si disponibles)
    val geoLocation: GeoLocation? = null,

    // Informations sur l'appareil
    val deviceInfo: DeviceInfo? = null,

    // Hash pour l'intégrité
    val dataHash: String? = null,

    // Informations de sécurité
    val securityContext: SecurityContext? = null
) {
    enum class AuditActionType(val displayName: String, val category: String) {
        // Authentication Actions
        LOGIN_SUCCESS("Connexion réussie", "AUTH"),
        LOGIN_FAILED("Tentative de connexion échouée", "AUTH"),
        LOGOUT("Déconnexion", "AUTH"),
        PASSWORD_CHANGED("Mot de passe modifié", "AUTH"),
        ACCOUNT_LOCKED("Compte verrouillé", "AUTH"),
        ACCOUNT_UNLOCKED("Compte déverrouillé", "AUTH"),

        // System Actions
        SYSTEM_CONFIG_CHANGE("Modification de configuration système", "SYSTEM"),
        DATABASE_BACKUP("Sauvegarde de base de données", "SYSTEM"),
        DATABASE_RESTORE("Restauration de base de données", "SYSTEM");
        
        companion object {
            fun fromString(action: String): AuditActionType? {
                return entries.find { it.name == action }
            }
        }
    }

}