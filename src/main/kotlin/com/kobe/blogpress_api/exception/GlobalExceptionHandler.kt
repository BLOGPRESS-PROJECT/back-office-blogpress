package com.kobe.blogpress_api.exception

import com.kobe.blogpress_api.dto.common.ApiResponseDto
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import java.util.*

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(ex: ResourceNotFoundException): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.warn("Resource not found: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ApiResponseDto.error(
                    message = ex.message ?: "Resource not found",
                    errorCode = "RESOURCE_NOT_FOUND",
                    requestId = UUID.randomUUID().toString()
                )
            )
    }

    @ExceptionHandler(ResourceAlreadyExistsException::class)
    fun handleResourceAlreadyExists(ex: ResourceAlreadyExistsException): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.warn("Resource already exists: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                ApiResponseDto.error(
                    message = ex.message ?: "Resource already exists",
                    errorCode = "RESOURCE_ALREADY_EXISTS",
                    requestId = UUID.randomUUID().toString()
                )
            )
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthentication(ex: AuthenticationException): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.warn("Authentication failed: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(
                ApiResponseDto.error(
                    message = ex.message ?: "Authentication failed",
                    errorCode = "AUTHENTICATION_FAILED",
                    requestId = UUID.randomUUID().toString()
                )
            )
    }

    @ExceptionHandler(AuthorizationException::class)
    fun handleAuthorization(ex: AuthorizationException): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.warn("Authorization failed: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(
                ApiResponseDto.error(
                    message = ex.message ?: "Access denied",
                    errorCode = "ACCESS_DENIED",
                    requestId = UUID.randomUUID().toString()
                )
            )
    }

    @ExceptionHandler(FileStorageException::class)
    fun handleFileStorage(ex: FileStorageException): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.error("File storage error: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ApiResponseDto.error(
                    message = ex.message ?: "File storage error",
                    errorCode = "FILE_STORAGE_ERROR",
                    requestId = UUID.randomUUID().toString()
                )
            )
    }

    @ExceptionHandler(ValidationException::class)
    fun handleValidation(ex: ValidationException): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.warn("Validation error: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ApiResponseDto.error(
                    message = ex.message ?: "Validation error",
                    errorCode = "VALIDATION_ERROR",
                    errorDetails = ex.errors,
                    requestId = UUID.randomUUID().toString()
                )
            )
    }

    @ExceptionHandler(WebExchangeBindException::class)
    fun handleValidationErrors(ex: WebExchangeBindException): ResponseEntity<ApiResponseDto<Nothing>> {
        val errors = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "Invalid value")
        }

        logger.warn("Validation errors: $errors")

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ApiResponseDto.error(
                    message = "Validation failed",
                    errorCode = "VALIDATION_ERROR",
                    errorDetails = errors,
                    requestId = UUID.randomUUID().toString()
                )
            )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.warn("Illegal argument: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ApiResponseDto.error(
                    message = ex.message ?: "Invalid argument",
                    errorCode = "INVALID_ARGUMENT",
                    requestId = UUID.randomUUID().toString()
                )
            )
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(ex: IllegalStateException): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.warn("Illegal state: ${ex.message}")
        // Vérifier si c'est une erreur liée à un blog non publié
        if (ex.message?.contains("not published", ignoreCase = true) == true) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                    ApiResponseDto.error(
                        message = "Le blog recherché n'est pas encore disponible",
                        errorCode = "BLOG_NOT_PUBLISHED",
                        requestId = UUID.randomUUID().toString()
                    )
                )
        }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ApiResponseDto.error(
                    message = ex.message ?: "Invalid state",
                    errorCode = "INVALID_STATE",
                    requestId = UUID.randomUUID().toString()
                )
            )
    }
    
    @ExceptionHandler(BlogNotFoundException::class)
    fun handleBlogNotFound(ex: BlogNotFoundException): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.warn("Blog not found: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ApiResponseDto.error(
                    message = ex.message ?: "Le blog recherché n'est pas disponible",
                    errorCode = "BLOG_NOT_FOUND",
                    requestId = UUID.randomUUID().toString()
                )
            )
    }
    
    @ExceptionHandler(BlogNotPublishedException::class)
    fun handleBlogNotPublished(ex: BlogNotPublishedException): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.warn("Blog not published: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ApiResponseDto.error(
                    message = ex.message ?: "Le blog recherché n'est pas encore disponible",
                    errorCode = "BLOG_NOT_PUBLISHED",
                    requestId = UUID.randomUUID().toString()
                )
            )
    }
    
    @ExceptionHandler(BlogPrivateException::class)
    fun handleBlogPrivate(ex: BlogPrivateException): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.warn("Blog is private: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(
                ApiResponseDto.error(
                    message = ex.message ?: "Ce blog est privé",
                    errorCode = "BLOG_PRIVATE",
                    requestId = UUID.randomUUID().toString()
                )
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.error("Unexpected error", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ApiResponseDto.error(
                    message = "An unexpected error occurred",
                    errorCode = "INTERNAL_SERVER_ERROR",
                    requestId = UUID.randomUUID().toString()
                )
            )
    }

    @ExceptionHandler(ContentNotYetPublishedException::class)
    fun handleContentNotYetPublished(ex: ContentNotYetPublishedException): ResponseEntity<ApiResponseDto<Nothing>> {
        logger.warn("Content not yet published: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ApiResponseDto.error(
                    message = "Le blog recherché n'est pas encore disponible",
                    errorCode = "CONTENT_NOT_YET_PUBLISHED",
                    errorDetails = mapOf(
                        "publishAt" to ex.publishAt.toString(),
                        "timeRemaining" to ex.getTimeRemaining()
                    ),
                    requestId = UUID.randomUUID().toString()
                )
            )
    }
}