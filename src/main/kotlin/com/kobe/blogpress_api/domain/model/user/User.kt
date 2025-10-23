package com.kobe.blogpress_api.domain.model.user

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "users")
@CompoundIndex(name = "idx_username_email", def = "{'username': 1, 'email': 1}")
data class User(
    @Id
    val id: ObjectId = ObjectId(),

    @Indexed(unique = true, name = "idx_username_unique")
    val username: String,

    @Indexed(unique = true, name = "idx_email_unique")
    val email: String,

    val password: String,

    val firstName: String,
    val lastName: String,

    val profilePicture: String? = null,

    val bio: String? = null,

    @Indexed(name = "idx_role")
    val role: Role = Role.USER,

    val socialLinks: SocialLinks = SocialLinks(),

    val statistics: UserStatistics = UserStatistics(),

    @Indexed(name = "idx_followers")
    val followers: Set<ObjectId> = emptySet(),

    @Indexed(name = "idx_following")
    val following: Set<ObjectId> = emptySet(),

    val isEmailVerified: Boolean = false,

    @Indexed(name = "idx_is_active")
    val isActive: Boolean = true,

    @Indexed(name = "idx_created_at", direction = org.springframework.data.mongodb.core.index.IndexDirection.DESCENDING)
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