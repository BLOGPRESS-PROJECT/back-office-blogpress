package com.kobe.blogpress_api.domain.dto.user

import com.kobe.blogpress_api.domain.model.user.Gender
import com.kobe.blogpress_api.domain.model.user.Role
import com.kobe.blogpress_api.domain.model.user.SocialLinks
import com.kobe.blogpress_api.domain.model.user.UserStatistics
import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.time.LocalDate

data class LoginRequestDTO(
    @field:NotBlank(message = "Email or username is required")
    val emailOrUsername: String,

    @field:NotBlank(message = "Password is required")
    val password: String
)

data class AuthResponseDTO(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: UserDTO
)

data class UserDTO(
    val id: String,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,

    // NOUVEAUX CHAMPS
    val birthDate: LocalDate? = null,
    val age: Int? = null,
    val gender: Gender? = null,
    val country: String? = null,
    val phoneNumber: String? = null,
    val interests: List<String> = emptyList(),
    val preferredLanguage: String = "fr",
    val isGoldenUser: Boolean = false,
    val goldenUserSince: Instant? = null,

    val profilePicture: String? = null,
    val bio: String? = null,
    val website: String? = null,
    val socialLinks: SocialLinks,
    val role: Role,
    val isEmailVerified: Boolean,
    val statistics: UserStatistics,
    val createdAt: Instant,
    val lastLoginAt: Instant? = null
)

data class RefreshTokenRequestDTO(
    val refreshToken: String
)

data class PublicUserDTO(
    val id: String,
    val username: String,
    val fullName: String,
    val profilePicture: String?,
    val bio: String?,
    val isGoldenUser: Boolean,
    val statistics: UserStatistics
)

data class PrivacyPreferencesDTO(
    val isPublic: Boolean,
    val showEmail: Boolean,
    val showLocation: Boolean
)
