package com.kobe.blogpress_api.controller.auth

import com.kobe.blogpress_api.dto.common.ApiResponseDto
import com.kobe.blogpress_api.dto.user.AuthResponseDTO
import com.kobe.blogpress_api.dto.user.LoginRequestDTO
import com.kobe.blogpress_api.dto.user.RefreshTokenRequestDTO
import com.kobe.blogpress_api.dto.user.RegisterRequestDTO
import com.kobe.blogpress_api.services.auth.AuthService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.util.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    @PostMapping("/register")
    fun register(
        @Valid @RequestBody registerRequest: RegisterRequestDTO
    ): Mono<ResponseEntity<ApiResponseDto<AuthResponseDTO>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Register request for email: ${registerRequest.email}")

        return authService.register(registerRequest)
            .map { authResponse ->
                logger.info("[$requestId] User registered successfully: ${authResponse.user.username}")
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(
                        ApiResponseDto.success(
                            data = authResponse,
                            message = "User registered successfully",
                            requestId = requestId
                        )
                    )
            }
            .doOnError { error ->
                logger.error("[$requestId] Registration failed: ${error.message}")
            }
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody loginRequest: LoginRequestDTO
    ): Mono<ResponseEntity<ApiResponseDto<AuthResponseDTO>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Login request for: ${loginRequest.emailOrUsername}")

        return authService.login(loginRequest)
            .map { authResponse ->
                logger.info("[$requestId] User logged in successfully: ${authResponse.user.username}")
                ResponseEntity.ok(
                    ApiResponseDto.success(
                        data = authResponse,
                        message = "Login successful",
                        requestId = requestId
                    )
                )
            }
            .doOnError { error ->
                logger.error("[$requestId] Login failed: ${error.message}")
            }
    }

    @PostMapping("/refresh")
    fun refreshToken(
        @Valid @RequestBody refreshRequest: RefreshTokenRequestDTO
    ): Mono<ResponseEntity<ApiResponseDto<AuthResponseDTO>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Refresh token request")

        return authService.refreshToken(refreshRequest.refreshToken)
            .map { authResponse ->
                logger.info("[$requestId] Token refreshed successfully for user: ${authResponse.user.username}")
                ResponseEntity.ok(
                    ApiResponseDto.success(
                        data = authResponse,
                        message = "Token refreshed successfully",
                        requestId = requestId
                    )
                )
            }
            .doOnError { error ->
                logger.error("[$requestId] Token refresh failed: ${error.message}")
            }
    }

    @PostMapping("/logout")
    fun logout(): Mono<ResponseEntity<ApiResponseDto<Nothing>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Logout request")

        // Avec JWT, le logout est géré côté client (suppression du token)
        // On peut implémenter une blacklist de tokens si nécessaire
        return Mono.just(
            ResponseEntity.ok(
                ApiResponseDto.success(
                    data = null,
                    message = "Logout successful. Please remove the token from client side.",
                    requestId = requestId
                )
            )
        ) as Mono<ResponseEntity<ApiResponseDto<Nothing>>>
    }
}