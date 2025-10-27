package com.kobe.blogpress_api.domain.model.user

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.time.LocalDate

@Document(collection = "users")
data class User(
    @Id
    val id: ObjectId = ObjectId(),

    @Indexed(unique = true)
    val username: String,

    @Indexed(unique = true)
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,

    // ===== NOUVEAUX CHAMPS - INFORMATIONS PERSONNELLES =====
    val birthDate: LocalDate? = null,
    val gender: Gender? = null,
    val country: String? = null,
    val phoneNumber: String? = null,
    val interests: List<String> = emptyList(),
    val preferredLanguage: String = "fr",

    // ===== PROFIL PUBLIC =====
    val profilePicture: String? = null,
    val bio: String? = null,
    val website: String? = null,
    val socialLinks: SocialLinks = SocialLinks(),

    // ===== RÔLE ET STATUT =====
    val role: Role = Role.USER,
    val isActive: Boolean = true,
    val isEmailVerified: Boolean = false,

    // ===== STATISTIQUES =====
    val statistics: UserStatistics = UserStatistics(),

    // ===== RELATIONS =====
    val followers: Set<ObjectId> = emptySet(),
    val following: Set<ObjectId> = emptySet(),

    // ===== DATES =====
    @Indexed
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val lastLoginAt: Instant? = null
)

data class SocialLinks(
    val twitter: String? = null,
    val linkedin: String? = null,
    val github: String? = null,
    val facebook: String? = null,
    val instagram: String? = null
)

data class UserStatistics(
    val totalPosts: Long = 0,
    val totalViews: Long = 0,
    val totalLikes: Long = 0,
    val totalComments: Long = 0,
    val followerCount: Long = 0,
    val followingCount: Long = 0
)

enum class Role {
    ADMIN,      // Gère les utilisateurs
    USER,       // Utilisateur connecté (CRUD sur ses contenus)
    MODERATOR   // Modération
}