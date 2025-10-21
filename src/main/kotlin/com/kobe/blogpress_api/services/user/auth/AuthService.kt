package com.kobe.blogpress_api.services.user.auth

import com.kobe.blogpress_api.configuration.HashEncoder
import com.kobe.blogpress_api.configuration.jwt.JwtService
import com.kobe.blogpress_api.model.user.Permission
import com.kobe.blogpress_api.model.user.RefreshToken
import com.kobe.blogpress_api.model.user.RolePermissionConfig
import com.kobe.blogpress_api.model.user.RoleType
import com.kobe.blogpress_api.model.user.User
import com.kobe.blogpress_api.repository.user.RefreshTokenRepository
import com.kobe.blogpress_api.repository.user.UserRepository
import com.kobe.blogpress_api.services.audit.AuditService
import com.kobe.blogpress_api.componentStarting.LoginAttemptService
import jakarta.servlet.http.HttpServletRequest
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64

@Component
class AuthService(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val hashEncoder: HashEncoder,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val auditService: AuditService,
    private val loginAttemptService: LoginAttemptService
) {
    data class TokenPair(
        val accessToken: String,
        val refreshToken: String,
        val user: UserResponse
    )

    data class UserResponse(
        val id: String,
        val email: String,
        val firstName: String,
        val lastName: String,
        val roleType: RoleType,
        val department: String,
        val permissions: Set<Permission>,
        val mustChangePassword: Boolean
    )

    data class CreateUserRequest(
        val email: String,
        val firstName: String,
        val lastName: String,
        val roleType: RoleType,
        val temporaryPassword: String? = null
    )

    // Création d'utilisateur (réservée aux administrateurs)
    @PreAuthorize("hasPermission(null, 'CREATE_USER')")
    fun createUser(request: CreateUserRequest, createdBy: String): User {
        val existingUser = userRepository.findByEmail(request.email.trim())
        if (existingUser != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Un utilisateur avec cet email existe déjà")
        }

        val creator = userRepository.findById(ObjectId(createdBy)).orElseThrow {
            ResponseStatusException(HttpStatus.UNAUTHORIZED, "Créateur non trouvé")
        }

        // Vérifier si le créateur peut assigner ce rôle
        if (!creator.roleType.canManageRole(request.roleType)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'avez pas l'autorisation d'assigner ce rôle")
        }

        // Déterminer si le createdBy doit être enregistré
        val createdById = if (creator.roleType == RoleType.ADMIN_SYSTEM) {
            null  // L'admin système est exempté.
        } else {
            ObjectId(createdBy)  // Pour tous les autres rôles
        }

        // Générer un mot de passe temporaire si non fourni
        val tempPassword = request.temporaryPassword ?: generateTemporaryPassword()

        val newUser = User(
            email = request.email.trim(),
            firstName = request.firstName.trim(),
            lastName = request.lastName.trim(),
            hashedPassword = hashEncoder.encode(tempPassword),
            roleType = request.roleType,
            mustChangePassword = true,
            createdBy = createdById  // Utilisation de la valeur conditionnelle
        )

        val savedUser = userRepository.save(newUser)

        // Audit log
        auditService.log(
            userId = ObjectId(createdBy),
            action = "CREATE_USER",
            resource = "User",
            resourceId = savedUser.id.toHexString(),
            details = mapOf(
                "email" to request.email,
                "roleType" to request.roleType.name,
                "department" to request.roleType.department,
                "createdBy" to (createdById?.toHexString() ?: "SYSTEM")
            )
        )
        return savedUser
    }

    // Connexion
    fun login(email: String, password: String, request: HttpServletRequest): TokenPair {

        val ipAddress = request.remoteAddr
        val userAgent = request.getHeader("User-Agent")

        // Vérifier les tentatives de connexion avant de procéder
        val attemptResult = loginAttemptService.checkLoginAttempt(email)
        if (attemptResult.isBlocked) {
            if (attemptResult.isPermanentlyDisabled) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, attemptResult.message)
            } else {
                throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, attemptResult.message)
            }
        }

        val user = userRepository.findByEmail(email.trim())
            ?: run {
                // Enregistrer la tentative même si l'utilisateur n'existe pas
                loginAttemptService.recordFailedAttempt(email, ipAddress, userAgent)
                throw BadCredentialsException("Identifiants invalides")
            }

        // Nouvelle logique de vérification de l'état du compte
        when {
            user.permanentlyDisabled -> {
                throw ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Votre compte a été définitivement désactivé. Contactez l'administrateur."
                )
            }
            !user.isActive -> {
                val message = when {
                    user.deactivationReason != null -> "Votre compte est désactivé : ${user.deactivationReason.description}"
                    user.blockedUntil != null && user.blockedUntil.isAfter(Instant.now()) ->
                        "Votre compte est temporairement bloqué jusqu'au ${user.blockedUntil}"
                    else -> "Votre compte est désactivé. Contactez l'administrateur."
                }
                throw ResponseStatusException(HttpStatus.FORBIDDEN, message)
            }
        }


        if (!user.isActive || user.permanentlyDisabled) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Votre compte est désactivé. ${user.deactivationReason?.description ?: "Contactez l'administrateur."}"
            )
        }

        if (!hashEncoder.matches(password, user.hashedPassword)) {
            // Enregistrer la tentative échouée
            val failedAttemptResult = loginAttemptService.recordFailedAttempt(email, ipAddress, userAgent)
            throw BadCredentialsException(failedAttemptResult.message)
        }

        // Connexion réussie - réinitialiser les tentatives
        loginAttemptService.recordSuccessfulLogin(email, ipAddress, userAgent)

        // Mettre à jour la dernière connexion
        userRepository.save(user.copy(lastLogin = Instant.now()))

        val accessToken = jwtService.generateAccessToken(user.id.toHexString(), user.roleType)
        val refreshToken = jwtService.generateRefreshToken(user.id.toHexString())

        storeRefreshToken(user.id, refreshToken)

        auditService.log(
            userId = user.id,
            action = "LOGIN_SUCCESS",
            resource = "Authentication",
            ipAddress = ipAddress,
            userAgent = userAgent
        )

        return TokenPair(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = toUserResponse(user)
        )
    }

    // Changement de mot de passe
    fun changePassword(userId: String, oldPassword: String, newPassword: String): Boolean {
        val user = userRepository.findById(ObjectId(userId)).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé")
        }

        if (!hashEncoder.matches(oldPassword, user.hashedPassword)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Ancien mot de passe incorrect")
        }

        val updatedUser = user.copy(
            hashedPassword = hashEncoder.encode(newPassword),
            mustChangePassword = false,
            updatedAt = Instant.now()
        )

        userRepository.save(updatedUser)

        // Audit
        auditService.log(
            userId = ObjectId(userId),
            action = "CHANGE_PASSWORD",
            resource = "User",
            resourceId = userId
        )

        return true
    }


    // Mise à jour du profil utilisateur (prénom/nom)
    fun updateUserProfile(userId: String, firstName: String?, lastName: String?): User {
        val user = userRepository.findById(ObjectId(userId)).orElseThrow {
            IllegalArgumentException("Utilisateur non trouvé")
        }

        val updatedUser = user.copy(
            firstName = firstName ?: user.firstName,
            lastName = lastName ?: user.lastName,
            updatedAt = Instant.now()
        )

        val savedUser = userRepository.save(updatedUser)

        auditService.log(
            userId = ObjectId(userId),
            action = "UPDATE_PROFILE",
            resource = "User",
            resourceId = userId,
            details = mapOf(
                "firstName" to (firstName != null),
                "lastName" to (lastName != null)
            )
        )

        return savedUser
    }

    /**
     * Reset de mot de passe par admin (génère un mot de passe temporaire)
     */
    fun resetPasswordByAdmin(userId: String, adminId: String): String {
        val user = userRepository.findById(ObjectId(userId)).orElseThrow {
            IllegalArgumentException("Utilisateur non trouvé")
        }

        val admin = userRepository.findById(ObjectId(adminId)).orElseThrow {
            ResponseStatusException(HttpStatus.UNAUTHORIZED, "Administrateur non trouvé")
        }

        val temporaryPassword = generateTemporaryPassword()
        val updatedUser = user.copy(
            hashedPassword = hashEncoder.encode(temporaryPassword),
            mustChangePassword = true,
            updatedAt = Instant.now()
        )

        userRepository.save(updatedUser)

        auditService.log(
            userId = ObjectId(adminId),
            action = "RESET_PASSWORD_BY_ADMIN",
            resource = "User",
            resourceId = userId,
            details = mapOf(
                "targetUser " to user.email,
                "resetBy " to admin.email
            )
        )

        return temporaryPassword
    }

    fun logout(userId: String, request: HttpServletRequest, jwt: String?) {
        val ip = request.remoteAddr
        val userAgent = request.getHeader("User-Agent") ?: "unknown"
        val logoutTime = Instant.now()

        // Audit détaillé de la déconnexion
        auditService.log(
            userId = ObjectId(userId),
            action = "LOGOUT",
            resource = "Authentication",
            details = mapOf(
                "ip" to ip,
                "userAgent" to userAgent,
                "logoutTime" to logoutTime.toString(),
                "jwt" to (jwt ?: "absent")
            )
        )
        // Ici, tu pourrais ajouter le token à une blacklist si tu veux l’invalider côté serveur
    }

    // Utilitaires
    private fun toUserResponse(user: User): UserResponse {
        return UserResponse(
            id = user.id.toHexString(),
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            roleType = user.roleType,
            department = user.department,
            permissions = RolePermissionConfig.getPermissions(user.roleType),
            mustChangePassword = user.mustChangePassword
        )
    }

    private fun generateTemporaryPassword(): String {
        //val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%"
        //return (1..12)
        //    .map { chars.random() }
        //    .joinToString("")
        return "RH-system78" // Mot de passe temporaire par défaut pour les tests
    }

    private fun storeRefreshToken(userId: ObjectId, rawRefreshToken: String) {
        val hashed = hashToken(rawRefreshToken)
        val expiryMs = jwtService.refreshTokenValidityMs
        val expiresAt = Instant.now().plusMillis(expiryMs)

        refreshTokenRepository.save(
            RefreshToken(
                userId = userId,
                expiresAt = expiresAt,
                hashedToken = hashed
            )
        )
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}