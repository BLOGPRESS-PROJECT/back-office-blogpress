package com.kobe.blogpress_api.dto.user

import com.kobe.blogpress_api.domain.model.user.Role
import com.kobe.blogpress_api.domain.model.user.SocialLinks
import com.kobe.blogpress_api.domain.model.user.UserStatistics
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import jakarta.validation.constraints.Pattern
import java.time.Instant

data class RegisterRequestDTO(
    @field:NotBlank(message = "Username is required")
    @field:Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
    @field:Pattern(
        regexp = "^[a-zA-Z0-9_-]+$",
        message = "Username can only contain letters, numbers, underscores and hyphens"
    )
    val username: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]+\$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one number and one special character"
    )
    val password: String,

    @field:NotBlank(message = "First name is required")
    @field:Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    val lastName: String,

    @field:Size(max = 500, message = "Bio cannot exceed 500 characters")
    val bio: String? = null,

    // Photo de profil optionnelle (URL externe ou sera uploadée séparément)
    val profilePictureUrl: String? = null
)

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
    val profilePicture: String?,
    val bio: String?,
    val role: Role,
    val socialLinks: SocialLinks,
    val statistics: UserStatistics,
    val isEmailVerified: Boolean,
    val createdAt: Instant,
    val lastLoginAt: Instant?
)

data class RefreshTokenRequestDTO(
    val refreshToken: String
)

data class UpdateProfileRequestDTO(
    val firstName: String?,
    val lastName: String?,
    val bio: String?,
    val socialLinks: SocialLinks?
)
