package com.kobe.blogpress_api.dto.user

import com.kobe.blogpress_api.domain.model.user.Gender
import jakarta.validation.constraints.*
import java.time.LocalDate

data class RegisterRequestDTO(
    // ===== OBLIGATOIRES =====
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
        message = "Password must contain at least one uppercase, one lowercase, one number and one special character"
    )
    val password: String,

    @field:NotBlank(message = "First name is required")
    @field:Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    val lastName: String,

    // ===== NOUVEAUX CHAMPS (OPTIONNELS) =====
    @field:Past(message = "Birth date must be in the past")
    val birthDate: LocalDate? = null,

    val gender: Gender? = null,

    @field:Size(max = 100, message = "Country cannot exceed 100 characters")
    val country: String? = null,

    @field:Pattern(
        regexp = "^[+]?[0-9]{10,15}$",
        message = "Phone number must be valid"
    )
    val phoneNumber: String? = null,

    @field:Size(max = 10, message = "Maximum 10 interests allowed")
    val interests: List<String>? = null,

    @field:Pattern(
        regexp = "^(fr|en|es|de|it|pt)$",
        message = "Preferred language must be: fr, en, es, de, it, or pt"
    )
    val preferredLanguage: String? = "fr",

    @field:Size(max = 500, message = "Bio cannot exceed 500 characters")
    val bio: String? = null,

    @field:Size(max = 200, message = "Website URL cannot exceed 200 characters")
    val website: String? = null,

    val profilePictureUrl: String? = null,
    val socialLinks: SocialLinksDTO? = null
)