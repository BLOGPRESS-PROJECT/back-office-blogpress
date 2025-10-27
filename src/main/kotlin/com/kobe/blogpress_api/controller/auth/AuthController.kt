package com.kobe.blogpress_api.controller.auth

import com.kobe.blogpress_api.dto.common.ApiResponseDto
import com.kobe.blogpress_api.dto.user.AuthResponseDTO
import com.kobe.blogpress_api.dto.user.LoginRequestDTO
import com.kobe.blogpress_api.dto.user.RefreshTokenRequestDTO
import com.kobe.blogpress_api.dto.user.RegisterRequestDTO
import com.kobe.blogpress_api.services.user.AuthService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    @PostMapping("/register")
    suspend fun register(
        @Valid @RequestBody registerRequest: RegisterRequestDTO
    ): ResponseEntity<ApiResponseDto<AuthResponseDTO>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Register request for email: ${registerRequest.email}")

        val authResponse = authService.register(registerRequest)

        logger.info("[$requestId] User registered successfully: ${authResponse.user.username}")
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                ApiResponseDto.success(
                    data = authResponse,
                    message = "User registered successfully",
                    requestId = requestId
                )
            )
    }

    @PostMapping("/register", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun registerWithProfilePicture(
        @RequestPart("user") @Valid registerRequest: RegisterRequestDTO,
        @RequestPart("profilePicture", required = false) profilePicture: FilePart?
    ): ResponseEntity<ApiResponseDto<AuthResponseDTO>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Register request for email: ${registerRequest.email}")

        val authResponse = authService.registerWithProfilePicture(registerRequest, profilePicture)

        logger.info("[$requestId] User registered successfully: ${authResponse.user.username}")
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                ApiResponseDto.success(
                    data = authResponse,
                    message = "User registered successfully",
                    requestId = requestId
                )
            )
    }

    @PostMapping("/login")
    suspend fun login(
        @Valid @RequestBody loginRequest: LoginRequestDTO
    ): ResponseEntity<ApiResponseDto<AuthResponseDTO>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Login request for: ${loginRequest.emailOrUsername}")

        val authResponse = authService.login(loginRequest)

        logger.info("[$requestId] User logged in successfully: ${authResponse.user.username}")
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = authResponse,
                message = "Login successful",
                requestId = requestId
            )
        )
    }

    @PostMapping("/refresh")
    suspend fun refreshToken(
        @Valid @RequestBody refreshRequest: RefreshTokenRequestDTO
    ): ResponseEntity<ApiResponseDto<AuthResponseDTO>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Refresh token request")

        val authResponse = authService.refreshToken(refreshRequest.refreshToken)

        logger.info("[$requestId] Token refreshed successfully for user: ${authResponse.user.username}")
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = authResponse,
                message = "Token refreshed successfully",
                requestId = requestId
            )
        )
    }

    @PostMapping("/logout")
    suspend fun logout(): ResponseEntity<ApiResponseDto<Nothing>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Logout request")

        // Avec JWT, le logout est géré côté client (suppression du token)
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = null,
                message = "Logout successful. Please remove the token from client side.",
                requestId = requestId
            )
        ) as ResponseEntity<ApiResponseDto<Nothing>>
    }
}