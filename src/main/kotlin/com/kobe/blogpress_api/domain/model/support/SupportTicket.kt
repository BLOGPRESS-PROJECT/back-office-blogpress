package com.kobe.blogpress_api.domain.model.support

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * Modèle pour les tickets de support.
 * Les Golden Users ont une priorité élevée.
 */
@Document(collection = "support_tickets")
data class SupportTicket(
    @Id
    val id: ObjectId = ObjectId(),

    @Indexed
    val userId: ObjectId,

    val subject: String,
    val description: String,
    val category: SupportCategory,
    val priority: TicketPriority, // Calculé automatiquement selon isGoldenUser

    val status: TicketStatus = TicketStatus.OPEN,
    val assignedTo: ObjectId? = null, // Admin assigné

    val isGoldenUser: Boolean = false, // Pour priorité automatique

    // Réponses et historique
    val messages: List<TicketMessage> = emptyList(),

    // Métadonnées
    val attachments: List<String> = emptyList(), // URLs des fichiers joints
    val tags: List<String> = emptyList(),

    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val resolvedAt: Instant? = null,
    val firstResponseAt: Instant? = null // Temps de première réponse (pour Golden Users < 4h)
)

data class TicketMessage(
    val id: ObjectId = ObjectId(),
    val authorId: ObjectId,
    val authorRole: String, // "USER", "ADMIN", "SUPPORT"
    val content: String,
    val attachments: List<String> = emptyList(),
    val createdAt: Instant = Instant.now(),
    val isInternal: Boolean = false // Note interne (visible seulement par le support)
)

enum class SupportCategory {
    TECHNICAL,      // Problème technique
    BILLING,        // Facturation
    FEATURE_REQUEST, // Demande de fonctionnalité
    BUG_REPORT,     // Signalement de bug
    ACCOUNT,        // Problème de compte
    OTHER           // Autre
}

enum class TicketPriority {
    LOW,        // Standard users
    MEDIUM,     // Standard users (urgent)
    HIGH,       // Golden Users (par défaut)
    URGENT      // Golden Users (urgent) ou Admin
}

enum class TicketStatus {
    OPEN,           // Ticket ouvert, en attente
    IN_PROGRESS,    // En cours de traitement
    WAITING_USER,   // En attente de réponse utilisateur
    RESOLVED,       // Résolu
    CLOSED          // Fermé
}

