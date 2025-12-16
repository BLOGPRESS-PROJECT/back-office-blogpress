package com.kobe.blogpress_api.dto.user

import com.kobe.blogpress_api.domain.model.user.Role
import java.time.Instant

/**
 * DTO léger pour la liste des utilisateurs côté admin.
 * Expose uniquement les informations nécessaires au tableau admin,
 * avec des dates réelles provenant du modèle `User`.
 */
data class AdminUserListItemDTO(
    val id: String,
    val username: String,
    val email: String,
    val fullName: String?,
    val role: Role,
    val isActive: Boolean,
    val isGoldenUser: Boolean,
    val goldenUserSince: Instant?,
    val createdAt: Instant,
    val lastLoginAt: Instant?,
    val statistics: com.kobe.blogpress_api.domain.model.user.UserStatistics?
)


