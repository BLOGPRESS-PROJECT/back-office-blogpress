package com.kobe.blogpress_api.startingComponents

import com.kobe.blogpress_api.model.user.DeactivationReason
import com.kobe.blogpress_api.model.user.User
import com.kobe.blogpress_api.repository.user.UserRepository
import com.kobe.blogpress_api.services.audit.AuditService
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class LoginAttemptService (
    private val userRepository: UserRepository,
    private val auditService: AuditService
){
    companion object {
        private const val INITIAL_WARNING_THRESHOLD = 3
        private const val TEMPORARY_BLOCK_THRESHOLD = 5
        private const val FINAL_WARNING_THRESHOLD = 7
        private const val PERMANENT_DISABLE_THRESHOLD = 9
        private const val TEMPORARY_BLOCK_DURATION_MINUTES = 5L //5 minutes
    }

    data class LoginAttemptResult(
        val isBlocked: Boolean,
        val remainingAttempts: Int,
        val message: String,
        val blockDuration: Long? = null,
        val isPermanentlyDisabled: Boolean = false
    )

    data class LoginAttempts(
        var attempts: Int = 1,
        var firstFailedAttempt: Instant = Instant.now(),
        var blockedUntil: Instant? = null
    )

    fun checkLoginAttempt(email: String): LoginAttemptResult {
        val user = userRepository.findByEmail(email.trim())
            ?: return LoginAttemptResult(
                isBlocked = false,
                remainingAttempts = INITIAL_WARNING_THRESHOLD,
                message = "Utilisateur non trouvé"
            )

        // Vérifier si l'utilisateur est définitivement désactivé
        if (user.isDeactivated) {
            return LoginAttemptResult(
                isBlocked = true,
                remainingAttempts = 0,
                message = "Votre compte est désactivé. ${user.deactivationReason?.description ?: "Contactez l'administrateur."}",
                isPermanentlyDisabled = true
            )
        }

        // Vérifier si l'utilisateur est temporairement bloqué
        if (user.isCurrentlyBlocked) {
            val minutesRemaining = java.time.Duration.between(
                Instant.now(),
                user.blockedUntil!!
            ).toMinutes()

            return LoginAttemptResult(
                isBlocked = true,
                remainingAttempts = 0,
                message = "Votre compte est temporairement bloqué. Réessayez dans $minutesRemaining minute(s).",
                blockDuration = minutesRemaining
            )
        }

        // Réinitialiser les tentatives si le blocage temporaire est expiré
        if (user.blockedUntil != null && user.blockedUntil.isBefore(Instant.now())) {
            resetTemporaryBlock(user)
        }

        return LoginAttemptResult(
            isBlocked = false,
            remainingAttempts = INITIAL_WARNING_THRESHOLD - user.loginAttempts,
            message = "Connexion autorisée"
        )
    }

    fun recordFailedAttempt(email: String, ipAddress: String?, userAgent: String?): LoginAttemptResult {
        val user = userRepository.findByEmail(email.trim())
            ?: return LoginAttemptResult(
                isBlocked = false,
                remainingAttempts = INITIAL_WARNING_THRESHOLD,
                message = "Utilisateur non trouvé"
            )

        val newAttempts = user.loginAttempts + 1
        val now = Instant.now()

        when (newAttempts) {
            // Premier avertissement à 3 tentatives
            INITIAL_WARNING_THRESHOLD -> {
                val updatedUser = user.copy(
                    loginAttempts = newAttempts,
                    lastFailedLoginAttempt = now,
                    updatedAt = now
                )
                userRepository.save(updatedUser)

                auditService.log(
                    userId = user.id,
                    action = "LOGIN_ATTEMPT_WARNING",
                    resource = "Authentication",
                    details = mapOf(
                        "attempts" to newAttempts,
                        "warningLevel" to "INITIAL"
                    ),
                    ipAddress = ipAddress,
                    userAgent = userAgent,
                    success = false
                )

                return LoginAttemptResult(
                    isBlocked = false,
                    remainingAttempts = TEMPORARY_BLOCK_THRESHOLD - newAttempts,
                    message = "Attention ! Il vous reste ${TEMPORARY_BLOCK_THRESHOLD - newAttempts} tentatives avant une suspension temporaire de 5 minutes."
                )
            }

            // Blocage temporaire à 5 tentatives
            TEMPORARY_BLOCK_THRESHOLD -> {
                val blockUntil = now.plus(TEMPORARY_BLOCK_DURATION_MINUTES, java.time.temporal.ChronoUnit.MINUTES)
                val updatedUser = user.copy(
                    loginAttempts = newAttempts,
                    lastFailedLoginAttempt = now,
                    blockedUntil = blockUntil,
                    updatedAt = now
                )
                userRepository.save(updatedUser)

                auditService.log(
                    userId = user.id,
                    action = "LOGIN_TEMPORARY_BLOCK",
                    resource = "Authentication",
                    details = mapOf(
                        "attempts" to newAttempts,
                        "blockDuration" to TEMPORARY_BLOCK_DURATION_MINUTES,
                        "blockUntil" to blockUntil.toString()
                    ),
                    ipAddress = ipAddress,
                    userAgent = userAgent,
                    success = false
                )

                return LoginAttemptResult(
                    isBlocked = true,
                    remainingAttempts = 0,
                    message = "Votre compte est temporairement bloqué pour $TEMPORARY_BLOCK_DURATION_MINUTES minutes en raison de tentatives de connexion répétées.",
                    blockDuration = TEMPORARY_BLOCK_DURATION_MINUTES
                )
            }

            // Avertissement final à 7 tentatives
            FINAL_WARNING_THRESHOLD -> {
                val updatedUser = user.copy(
                    loginAttempts = newAttempts,
                    lastFailedLoginAttempt = now,
                    updatedAt = now
                )
                userRepository.save(updatedUser)

                auditService.log(
                    userId = user.id,
                    action = "LOGIN_FINAL_WARNING",
                    resource = "Authentication",
                    details = mapOf(
                        "attempts" to newAttempts,
                        "warningLevel" to "FINAL"
                    ),
                    ipAddress = ipAddress,
                    userAgent = userAgent,
                    success = false
                )

                return LoginAttemptResult(
                    isBlocked = false,
                    remainingAttempts = PERMANENT_DISABLE_THRESHOLD - newAttempts,
                    message = "ATTENTION ! Votre compte risque d'être désactivé définitivement. Il vous reste ${PERMANENT_DISABLE_THRESHOLD - newAttempts} tentatives."
                )
            }

            // Désactivation définitive à 9 tentatives
            PERMANENT_DISABLE_THRESHOLD -> {
                val updatedUser = user.copy(
                    loginAttempts = newAttempts,
                    lastFailedLoginAttempt = now,
                    isActive = false,
                    permanentlyDisabled = true,
                    deactivationReason = DeactivationReason.SECURITY_BREACH,
                    deactivatedAt = now,
                    deactivationComment = "Compte désactivé automatiquement après $newAttempts tentatives de connexion échouées",
                    updatedAt = now
                )
                userRepository.save(updatedUser)

                auditService.log(
                    userId = user.id,
                    action = "LOGIN_PERMANENT_DISABLE",
                    resource = "Authentication",
                    details = mapOf(
                        "attempts" to newAttempts,
                        "reason" to DeactivationReason.SECURITY_BREACH.name,
                        "autoDisabled" to true
                    ),
                    ipAddress = ipAddress,
                    userAgent = userAgent,
                    success = false
                )

                return LoginAttemptResult(
                    isBlocked = true,
                    remainingAttempts = 0,
                    message = "Votre compte a été désactivé définitivement en raison de tentatives de connexion suspectes. Contactez l'administrateur.",
                    isPermanentlyDisabled = true
                )
            }

            // Autres tentatives
            else -> {
                val updatedUser = user.copy(
                    loginAttempts = newAttempts,
                    lastFailedLoginAttempt = now,
                    updatedAt = now
                )
                userRepository.save(updatedUser)

                auditService.log(
                    userId = user.id,
                    action = "LOGIN_FAILED",
                    resource = "Authentication",
                    details = mapOf("attempts" to newAttempts),
                    ipAddress = ipAddress,
                    userAgent = userAgent,
                    success = false
                )

                val remaining = when {
                    newAttempts < INITIAL_WARNING_THRESHOLD -> INITIAL_WARNING_THRESHOLD - newAttempts
                    newAttempts < TEMPORARY_BLOCK_THRESHOLD -> TEMPORARY_BLOCK_THRESHOLD - newAttempts
                    newAttempts < FINAL_WARNING_THRESHOLD -> FINAL_WARNING_THRESHOLD - newAttempts
                    else -> PERMANENT_DISABLE_THRESHOLD - newAttempts
                }

                return LoginAttemptResult(
                    isBlocked = false,
                    remainingAttempts = remaining,
                    message = "Identifiants incorrects. Tentatives restantes: $remaining"
                )
            }
        }
    }

    fun recordSuccessfulLogin(email: String, ipAddress: String?, userAgent: String?) {
        val user = userRepository.findByEmail(email.trim()) ?: return

        if (user.loginAttempts > 0 || user.blockedUntil != null) {
            val updatedUser = user.copy(
                loginAttempts = 0,
                lastFailedLoginAttempt = null,
                blockedUntil = null,
                lastLogin = Instant.now(),
                updatedAt = Instant.now()
            )
            userRepository.save(updatedUser)

            auditService.log(
                userId = user.id,
                action = "LOGIN_SUCCESS_RESET_ATTEMPTS",
                resource = "Authentication",
                details = mapOf(
                    "previousAttempts" to user.loginAttempts,
                    "wasBlocked" to (user.blockedUntil != null)
                ),
                ipAddress = ipAddress,
                userAgent = userAgent
            )
        }
    }

    private fun resetTemporaryBlock(user: User) {
        val updatedUser = user.copy(
            blockedUntil = null,
            loginAttempts = TEMPORARY_BLOCK_THRESHOLD, // Garde les tentatives pour continuer la progression
            updatedAt = Instant.now()
        )
        userRepository.save(updatedUser)
    }
}