package com.kobe.blogpress_api.model.user

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("users")
data class User(
    val email: String,
    val hashedPassword: String,
    @Id val id: ObjectId = ObjectId(),
    val firstName: String,
    val lastName: String,
    val roleType: RoleType,
    val isActive: Boolean = true,
    val mustChangePassword: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val createdBy: ObjectId? = null,
    val lastLogin: Instant? = null,

    // Nouveaux champs pour la gestion des tentatives de connexion
    val loginAttempts: Int = 0,
    val lastFailedLoginAttempt: Instant? = null,
    val blockedUntil: Instant? = null,
    val permanentlyDisabled: Boolean = false,

    // Champs pour la gestion de la désactivation
    val deactivationReason: DeactivationReason? = null,
    val deactivatedBy: ObjectId? = null,
    val deactivatedAt: Instant? = null,
    val deactivationComment: String? = null
){
    val department: String
        get() = roleType.department

    val isCurrentlyBlocked: Boolean
        get() = blockedUntil?.isAfter(Instant.now()) == true

    val isDeactivated: Boolean
        get() = !isActive || permanentlyDisabled
}