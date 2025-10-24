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
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val userService: UserService
) {

    suspend fun register(registerRequest: RegisterRequestDTO): AuthResponseDTO {
        checkUserExists(registerRequest.email, registerRequest.username)

        val user = User(
            username = registerRequest.username.lowercase(),
            email = registerRequest.email.lowercase(),
            password = passwordEncoder.encode(registerRequest.password),
            firstName = registerRequest.firstName,
            lastName = registerRequest.lastName,
            bio = registerRequest.bio,
            role = Role.USER
        )

        val savedUser = userRepository.save(user).awaitSingle()
        return generateAuthResponse(savedUser)
    }

    suspend fun login(loginRequest: LoginRequestDTO): AuthResponseDTO {
        val user = findUserByEmailOrUsername(loginRequest.emailOrUsername)

        if (!user.isActive) {
            throw AuthenticationException("Account is deactivated")
        }

        if (!passwordEncoder.matches(loginRequest.password, user.password)) {
            throw AuthenticationException("Invalid credentials")
        }

        // Mettre à jour lastLoginAt
        val updatedUser = user.copy(lastLoginAt = Instant.now())
        val savedUser = userRepository.save(updatedUser).awaitSingle()

        return generateAuthResponse(savedUser)
    }

    suspend fun refreshToken(refreshToken: String): AuthResponseDTO {
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

        val userId = jwtService.extractUserId(refreshToken)
        val user = userService.findById(userId)

        return generateAuthResponse(user)
    }

    private fun generateAuthResponse(user: User): AuthResponseDTO {
        val accessToken = jwtService.generateAccessToken(user.id, user.email, user.role)
        val refreshToken = jwtService.generateRefreshToken(user.id)

        return AuthResponseDTO(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = jwtService.getAccessTokenExpiration() / 1000,
            user = userService.toDTO(user)
        )
    }

    private suspend fun checkUserExists(email: String, username: String) {
        val emailExists = userRepository.existsByEmail(email.lowercase()).awaitSingle()
        if (emailExists) {
            throw ResourceAlreadyExistsException("Email already exists")
        }

        val usernameExists = userRepository.existsByUsername(username.lowercase()).awaitSingle()
        if (usernameExists) {
            throw ResourceAlreadyExistsException("Username already exists")
        }
    }

    private suspend fun findUserByEmailOrUsername(emailOrUsername: String): User {
        val normalized = emailOrUsername.lowercase()

        return if (normalized.contains("@")) {
            userRepository.findByEmail(normalized).awaitSingleOrNull()
        } else {
            userRepository.findByUsername(normalized).awaitSingleOrNull()
        } ?: throw AuthenticationException("Invalid credentials")
    }
}