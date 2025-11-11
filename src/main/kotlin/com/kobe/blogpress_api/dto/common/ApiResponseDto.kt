package com.kobe.blogpress_api.dto.common

import java.time.Instant

data class ApiResponseDto<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val timestamp: Instant = Instant.now(),
    val errorCode: String? = null,
    val errorDetails: Map<String, Any>? = null,
    val warnings: List<String>? = null,
    val metadata: Map<String, Any>? = null,
    val requestId: String? = null,
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