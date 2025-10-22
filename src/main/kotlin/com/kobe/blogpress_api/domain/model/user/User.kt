package com.kobe.blogpress_api.domain.model.user

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

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

    val profilePicture: String? = null,

    val bio: String? = null,

    val role: Role = Role.USER,

    val socialLinks: SocialLinks = SocialLinks(),

    val statistics: UserStatistics = UserStatistics(),

    val followers: Set<ObjectId> = emptySet(),
    val following: Set<ObjectId> = emptySet(),

    val isEmailVerified: Boolean = false,
    val isActive: Boolean = true,

    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),

    val lastLoginAt: Instant? = null
)

enum class Role {
    ADMIN,      // Gère les utilisateurs
    USER,       // Utilisateur connecté (CRUD sur ses contenus)
    MODERATOR   // Modération
}

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