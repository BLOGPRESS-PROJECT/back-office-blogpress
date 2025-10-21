package com.kobe.blogpress_api.services.user.auth

import com.kobe.blogpress_api.model.user.DeactivationReason
import com.kobe.blogpress_api.model.user.User
import com.kobe.blogpress_api.repository.user.UserRepository
import com.kobe.blogpress_api.services.audit.AuditService
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@Service
class UserDeactivationService(
    private val userRepository: UserRepository,
    private val auditService: AuditService
) {

    fun deactivateUser(
        userId: String,
        adminId: String,
        reason: DeactivationReason,
        comment: String? = null
    ): User {
        val user = userRepository.findById(ObjectId(userId)).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé")
        }

        val admin = userRepository.findById(ObjectId(adminId)).orElseThrow {
            ResponseStatusException(HttpStatus.UNAUTHORIZED, "Administrateur non trouvé")
        }

        val now = Instant.now()
        val updatedUser = user.copy(
            isActive = false,
            deactivationReason = reason,
            deactivatedBy = ObjectId(adminId),
            deactivatedAt = now,
            deactivationComment = comment,
            updatedAt = now
        )

        val savedUser = userRepository.save(updatedUser)

        auditService.log(
            userId = ObjectId(adminId),
            action = "DEACTIVATE_USER",
            resource = "User",
            resourceId = userId,
            details = mapOf(
                "reason" to reason.name,
                "reasonDescription" to reason.description,
                "comment" to comment,
                "deactivatedUser" to user.email,
                "deactivatedBy" to admin.email
            ) as Map<String, Any>
        )

        return savedUser
    }

    fun reactivateUser(userId: String, adminId: String, comment: String? = null): User {
        val user = userRepository.findById(ObjectId(userId)).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé")
        }

        val admin = userRepository.findById(ObjectId(adminId)).orElseThrow {
            ResponseStatusException(HttpStatus.UNAUTHORIZED, "Administrateur non trouvé")
        }

        val now = Instant.now()
        val updatedUser = user.copy(
            isActive = true,
            permanentlyDisabled = false,
            loginAttempts = 0,
            lastFailedLoginAttempt = null,
            blockedUntil = null,
            deactivationReason = null,
            deactivatedBy = null,
            deactivatedAt = null,
            deactivationComment = null,
            updatedAt = now
        )

        val savedUser = userRepository.save(updatedUser)

        auditService.log(
            userId = ObjectId(adminId),
            action = "REACTIVATE_USER",
            resource = "User",
            resourceId = userId,
            details = mapOf(
                "comment" to comment,
                "reactivatedUser" to user.email,
                "reactivatedBy" to admin.email,
                "previousReason" to user.deactivationReason?.name
            ) as Map<String, Any>
        )

        return savedUser
    }

}