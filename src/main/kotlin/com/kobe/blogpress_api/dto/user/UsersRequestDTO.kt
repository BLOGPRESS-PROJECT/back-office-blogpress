package com.kobe.blogpress_api.dto.user

import com.kobe.blogpress_api.model.user.DeactivationReason
import org.bson.types.ObjectId
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.jetbrains.annotations.NotNull
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

data class UpdateUserRequest(
    val email: String? = null,
    val newPassword: String? = null,
    val firstName: String? = null,
    val lastName: String? = null
)

data class CreateUserRequestDto(
    val requestType: RequestType,
    val motif: String,
    val requestData: Map<String, Any> = mapOf()
)

data class ReviewRequestDto(
    val approved: Boolean,
    val reviewComment: String? = null
)

data class UserRequestSummary(
    val id: String,
    val userId: String,
    val userEmail: String,
    val userName: String,
    val requestType: RequestType,
    val motif: String,
    val status: RequestStatus,
    val requestData: Map<String, Any>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val reviewedBy: String? = null,
    val reviewedAt: Instant? = null,
    val reviewComment: String? = null
)


// DTOs pour la gestion des désactivations
data class DeactivateUserRequest(
    @field:NotNull(message = "La raison de désactivation est requise")
    val reason: DeactivationReason,

    @field:Size(max = 500, message = "Le commentaire ne peut pas dépasser 500 caractères")
    val comment: String? = null
)

data class ReactivateUserRequest(
    @field:Size(max = 500, message = "Le commentaire ne peut pas dépasser 500 caractères")
    val comment: String? = null
)

data class UserDeactivationInfo(
    val isActive: Boolean,
    val permanentlyDisabled: Boolean,
    val deactivationReason: DeactivationReason?,
    val deactivatedBy: String?,
    val deactivatedAt: Instant?,
    val deactivationComment: String?,
    val loginAttempts: Int,
    val lastFailedLoginAttempt: Instant?,
    val blockedUntil: Instant?
)
//===================================================================================================

enum class RequestType(val description: String) {
    PASSWORD_RESET("Demande de réinitialisation de mot de passe"), // Pour utilisateur non connecté
    PASSWORD_CHANGE("Demande de changement de mot de passe"), // Pour utilisateur connecté (si nécessaire)
    EMAIL_CHANGE("Demande de changement d'email"),
    PROFILE_UPDATE("Demande de mise à jour du profil"),
    ROLE_CHANGE("Demande de changement de rôle"),
    DEPARTMENT_TRANSFER("Demande de transfert de département"),
    ACCOUNT_ACTIVATION("Demande d'activation de compte"),
    PERMISSION_REQUEST("Demande de permission spéciale"),
    ACCOUNT_DELETION("Demande de suppression de compte"),
    ACCESS_REQUEST("Demande d'accès à une ressource"),
    // Autres types de requêtes personnalisées...
}

enum class RequestStatus(val description: String) {
    PENDING("En attente"),
    APPROVED("Approuvée"),
    REJECTED("Rejetée"),
    CANCELLED("Annulée"),
    EXPIRED("Expirée")
}

@Document("user_requests")
data class UserRequest(
    @Id val id: ObjectId = ObjectId(),
    val userId: ObjectId,
    val requestType: RequestType,
    val motif: String,
    val status: RequestStatus = RequestStatus.PENDING,
    val requestData: Map<String, Any> = mapOf(), // Données spécifiques à la requête
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val reviewedBy: ObjectId? = null,
    val reviewedAt: Instant? = null,
    val reviewComment: String? = null,
    val expiresAt: Instant? = null
)