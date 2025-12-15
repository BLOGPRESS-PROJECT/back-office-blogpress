package com.kobe.blogpress_api.services.user

import com.kobe.blogpress_api.configuration.security.jwt.JwtService
import com.kobe.blogpress_api.domain.model.user.Role
import com.kobe.blogpress_api.domain.model.user.SocialLinks
import com.kobe.blogpress_api.domain.model.user.User
import com.kobe.blogpress_api.dto.user.AuthResponseDTO
import com.kobe.blogpress_api.dto.user.LoginRequestDTO
import com.kobe.blogpress_api.dto.user.RegisterRequestDTO
import com.kobe.blogpress_api.exception.AuthenticationException
import com.kobe.blogpress_api.exception.ResourceAlreadyExistsException
import com.kobe.blogpress_api.repository.user.UserRepository
import com.kobe.blogpress_api.services.fileStorage.FileStorageService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val userService: UserService,
    private val fileStorageService: FileStorageService
) {

    suspend fun register(registerRequest: RegisterRequestDTO): AuthResponseDTO {
        checkUserExists(registerRequest.email, registerRequest.username)

        val user = User(
            username = registerRequest.username.lowercase(),
            email = registerRequest.email.lowercase(),
            password = passwordEncoder.encode(registerRequest.password),
            firstName = registerRequest.firstName,
            lastName = registerRequest.lastName,
            birthDate = registerRequest.birthDate,
            gender = registerRequest.gender,
            country = registerRequest.country,
            phoneNumber = registerRequest.phoneNumber,
            interests = registerRequest.interests ?: emptyList(),
            preferredLanguage = registerRequest.preferredLanguage ?: "fr",
            bio = registerRequest.bio,
            website = registerRequest.website,
            profilePicture = registerRequest.profilePictureUrl,
            socialLinks = registerRequest.socialLinks?.let {
                SocialLinks(
                    twitter = it.twitter,
                    linkedin = it.linkedin,
                    github = it.github,
                    facebook = it.facebook,
                    instagram = it.instagram
                )
            } ?: SocialLinks(),
            role = Role.USER
        )

        val savedUser = userRepository.save(user).awaitSingle()
        return generateAuthResponse(savedUser)
    }

    suspend fun registerWithProfilePicture(
        registerRequest: RegisterRequestDTO,
        profilePicture: FilePart?
    ): AuthResponseDTO {
        checkUserExists(registerRequest.email, registerRequest.username)

        val profilePictureUrl = if (profilePicture != null) {
            val tempUserId = java.util.UUID.randomUUID().toString()
            fileStorageService.storeProfilePicture(profilePicture, tempUserId)
        } else {
            registerRequest.profilePictureUrl
        }

        val user = User(
            username = registerRequest.username.lowercase(),
            email = registerRequest.email.lowercase(),
            password = passwordEncoder.encode(registerRequest.password),
            firstName = registerRequest.firstName,
            lastName = registerRequest.lastName,
            birthDate = registerRequest.birthDate,
            gender = registerRequest.gender,
            country = registerRequest.country,
            phoneNumber = registerRequest.phoneNumber,
            interests = registerRequest.interests ?: emptyList(),
            preferredLanguage = registerRequest.preferredLanguage ?: "fr",
            bio = registerRequest.bio,
            website = registerRequest.website,
            profilePicture = profilePictureUrl,
            socialLinks = registerRequest.socialLinks?.let {
                SocialLinks(
                    twitter = it.twitter,
                    linkedin = it.linkedin,
                    github = it.github,
                    facebook = it.facebook,
                    instagram = it.instagram
                )
            } ?: SocialLinks(),
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

        return generateAuthResponse(savedUser, loginRequest.rememberMe)
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

        // On ne connaît pas explicitement rememberMe ici, mais il est encodé dans le token
        // On peut le réutiliser pour garder un comportement cohérent
        val claimsRememberMe = try {
            (jwtService.extractAllClaimsInternal(refreshToken)["rememberMe"] as? Boolean) ?: false
        } catch (e: Exception) {
            false
        }

        return generateAuthResponse(user, claimsRememberMe)
    }

    private suspend fun generateAuthResponse(user: User, rememberMe: Boolean = false): AuthResponseDTO {
        val accessToken = jwtService.generateAccessToken(user.id, user.email, user.role)
        val refreshToken = jwtService.generateRefreshToken(user.id, rememberMe)

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