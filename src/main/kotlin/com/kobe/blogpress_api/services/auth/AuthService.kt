package com.kobe.blogpress_api.services.auth

import com.kobe.blogpress_api.configuration.security.jwt.JwtService
import com.kobe.blogpress_api.domain.model.user.Role
import com.kobe.blogpress_api.domain.model.user.User
import com.kobe.blogpress_api.dto.user.AuthResponseDTO
import com.kobe.blogpress_api.dto.user.LoginRequestDTO
import com.kobe.blogpress_api.dto.user.RegisterRequestDTO
import com.kobe.blogpress_api.exception.AuthenticationException
import com.kobe.blogpress_api.exception.ResourceAlreadyExistsException
import com.kobe.blogpress_api.repository.user.UserRepository
import com.kobe.blogpress_api.services.user.UserService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val userService: UserService
) {

    fun register(registerRequest: RegisterRequestDTO): Mono<AuthResponseDTO> {
        return checkUserExists(registerRequest.email, registerRequest.username)
            .flatMap {
                val user = User(
                    username = registerRequest.username.lowercase(),
                    email = registerRequest.email.lowercase(),
                    password = passwordEncoder.encode(registerRequest.password),
                    firstName = registerRequest.firstName,
                    lastName = registerRequest.lastName,
                    bio = registerRequest.bio,
                    role = Role.USER
                )
                userRepository.save(user)
            }
            .flatMap { user -> generateAuthResponse(user) }
    }

    fun login(loginRequest: LoginRequestDTO): Mono<AuthResponseDTO> {
        return findUserByEmailOrUsername(loginRequest.emailOrUsername)
            .flatMap { user ->
                if (!user.isActive) {
                    return@flatMap Mono.error<User>(
                        AuthenticationException("Account is deactivated")
                    )
                }

                if (!passwordEncoder.matches(loginRequest.password, user.password)) {
                    return@flatMap Mono.error<User>(
                        AuthenticationException("Invalid credentials")
                    )
                }

                // Mettre à jour lastLoginAt
                val updatedUser = user.copy(lastLoginAt = Instant.now())
                userRepository.save(updatedUser)
            }
            .flatMap { user -> generateAuthResponse(user) }
    }

    fun refreshToken(refreshToken: String): Mono<AuthResponseDTO> {
        return Mono.fromCallable {
            if (!jwtService.validateToken(refreshToken)) {
                throw AuthenticationException("Invalid refresh token")
            }

            if (jwtService.isTokenExpired(refreshToken)) {
                throw AuthenticationException("Refresh token expired")
            }

            val tokenType = jwtService.extractTokenType(refreshToken)
            if (tokenType != "REFRESH") {
                throw AuthenticationException("Invalid token type")
            }

            jwtService.extractUserId(refreshToken)
        }
            .flatMap { userId -> userService.findById(userId) }
            .flatMap { user -> generateAuthResponse(user) }
    }

    private fun generateAuthResponse(user: User): Mono<AuthResponseDTO> {
        return Mono.fromCallable {
            val accessToken = jwtService.generateAccessToken(user.id, user.email, user.role)
            val refreshToken = jwtService.generateRefreshToken(user.id)

            AuthResponseDTO(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresIn = jwtService.getAccessTokenExpiration() / 1000,
                user = userService.toDTO(user)
            )
        }
    }

    private fun checkUserExists(email: String, username: String): Mono<Boolean> {
        return userRepository.existsByEmail(email.lowercase())
            .flatMap { emailExists ->
                if (emailExists) {
                    Mono.error(ResourceAlreadyExistsException("Email already exists"))
                } else {
                    userRepository.existsByUsername(username.lowercase())
                }
            }
            .flatMap { usernameExists ->
                if (usernameExists) {
                    Mono.error(ResourceAlreadyExistsException("Username already exists"))
                } else {
                    Mono.just(true)
                }
            }
    }

    private fun findUserByEmailOrUsername(emailOrUsername: String): Mono<User> {
        val normalized = emailOrUsername.lowercase()
        return if (normalized.contains("@")) {
            userRepository.findByEmail(normalized)
        } else {
            userRepository.findByUsername(normalized)
        }.switchIfEmpty(
            Mono.error(AuthenticationException("Invalid credentials"))
        )
    }
}