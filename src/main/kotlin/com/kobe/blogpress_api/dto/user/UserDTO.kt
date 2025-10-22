package com.kobe.blogpress_api.dto.user

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class ChangePasswordRequest(
    @field:NotBlank(message = "L'ancien mot de passe est requis")
    val oldPassword: String,

    @field:NotBlank(message = "Le nouveau mot de passe est requis")
    @field:Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    val newPassword: String
)

data class UpdateProfileRequest(
    @field:Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
    val firstName: String?,

    @field:Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    val lastName: String?
)

data class ForgotPasswordRequest(
    @field:Email(message = "Format d'email invalide")
    @field:NotBlank(message = "L'email est requis")
    val email: String,

    @field:NotBlank(message = "Le motif est requis")
    @field:Size(min = 30, max = 1000, message = "Le motif doit contenir entre 30 et 1000 caractères")
    val motif: String
)

/**
 * DTO pour les réponses d'API standardisées
 */
data class ApiResponseDto<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val timestamp: Instant = Instant.now(),

    val errorCode: String? = null,
    val errorDetails: Map<String, Any>? = null,
    val warnings: List<String>? = null,
    val metadata: Map<String, Any>? = null,
    val requestId: String? = null, // ✅ Pour tracer les requêtes
    val version: String = "1.0"
) {
    companion object {
        fun <T> success(
            data: T,
            message: String,
            warnings: List<String>? = null,
            metadata: Map<String, Any>? = null,
            requestId: String? = null
        ): ApiResponseDto<T> {
            return ApiResponseDto(
                success = true,
                message = message,
                data = data,
                warnings = warnings,
                metadata = metadata,
                requestId = requestId
            )
        }

        fun <T> error(
            message: String,
            errorCode: String? = null,
            errorDetails: Map<String, Any>? = null,
            requestId: String? = null
        ): ApiResponseDto<T> {
            return ApiResponseDto(
                success = false,
                message = message,
                data = null,
                errorCode = errorCode,
                errorDetails = errorDetails,
                requestId = requestId
            )
        }

        fun <T> partialSuccess(
            data: T,
            message: String,
            warnings: List<String>,
            requestId: String? = null
        ): ApiResponseDto<T> {
            return ApiResponseDto(
                success = true,
                message = message,
                data = data,
                warnings = warnings,
                requestId = requestId
            )
        }
    }
}