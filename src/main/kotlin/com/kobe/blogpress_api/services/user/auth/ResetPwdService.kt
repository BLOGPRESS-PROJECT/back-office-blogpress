package com.kobe.blogpress_api.services.user.auth

import com.kobe.blogpress_api.configuration.HashEncoder
import com.kobe.blogpress_api.model.user.PasswordResetHistory
import com.kobe.blogpress_api.model.user.ResetPwdRequest
import com.kobe.blogpress_api.model.user.ResetPwdStatus
import com.kobe.blogpress_api.repository.user.PasswordResetHistoryRepository
import com.kobe.blogpress_api.repository.user.ResetPwdRequestRepository
import com.kobe.blogpress_api.repository.user.UserRepository
import com.kobe.blogpress_api.services.audit.AuditService
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ResetPwdService(
    private val resetPwdRequestRepository: ResetPwdRequestRepository,
    private val passwordResetHistoryRepository: PasswordResetHistoryRepository,
    private val userRepository: UserRepository,
    private val auditService: AuditService,
    private val hashEncoder: HashEncoder,
    //private val mailService: MailService // À implémenter pour l'envoi d'email
){
    fun submitRequest(email : String, motif: String) {
        val user = userRepository.findByEmail(email = email)?: throw IllegalArgumentException("User with email $email not found")
        //val usered = userRepository.findById(ObjectId(userId)).orElseThrow()

        val pendingRequest = resetPwdRequestRepository
            .findByUserId(user.id)
            .any { it.status == ResetPwdStatus.PENDING }
        if (pendingRequest) {
            auditService.log(
                userId = user.id,
                action = "RESET_PWD_REQUEST_ALREADY_PENDING",
                resource = "ResetPwdRequest",
                resourceId = user.id.toHexString(),
                details = mapOf(
                    "motif" to motif,
                    "userEmail" to user.email,
                    "message" to "Demande déjà en attente"
                )
            )
            throw IllegalStateException("Une demande de réinitialisation est déjà en attente pour cet utilisateur.")
        }

        val request = ResetPwdRequest(
            userId = user.id,
            userEmail = user.email,
            motif = motif
        )
        resetPwdRequestRepository.save(request)
        auditService.log(
            userId = user.id,
            action = "RESET_PWD_REQUEST_SUBMITTED",
            resource = "ResetPwdRequest",
            resourceId = request.id.toHexString(),
            details = mapOf(
                "motif" to motif,
                "userEmail" to user.email,
                "requestDate" to request.date.toString()
            )
        )
    }

    fun acceptRequest(requestId: String, adminId: String, adminIp: String? = null) {
        val request = resetPwdRequestRepository.findById(ObjectId(requestId)).orElseThrow()
        val user = userRepository.findById(request.userId).orElseThrow()
        val oldPasswordHash = user.hashedPassword
        val newPassword = generateTemporaryPassword()

        userRepository.save(user.copy(
            hashedPassword = hashEncoder.encode(newPassword),
            mustChangePassword = true,
            updatedAt = Instant.now()
        ))
        resetPwdRequestRepository.save(request.copy(status = ResetPwdStatus.ACCEPTED))

        // Notification par email
        //mailService.sendPasswordReset(user.email, newPassword)

        // Enregistrer dans l'historique
        val history = PasswordResetHistory(
            userId = user.id,
            adminId = ObjectId(adminId),
            reason = request.motif,
            newPasswordHash = hashEncoder.encode(newPassword)
        )
        passwordResetHistoryRepository.save(history)

        auditService.log(
            userId = ObjectId(adminId),
            action = "RESET_PWD_ACCEPTED",
            resource = "PasswordResetHistory",
            resourceId = history.id.toHexString(),
            details = mapOf(
                "userEmail" to user.email,
                "resetRequestId" to request.id.toHexString(),
                "oldPasswordHash" to oldPasswordHash,
                "newPasswordHash" to hashEncoder.encode(newPassword),
                "resetDate" to history.resetDate.toString(),
                "adminIp" to (adminIp ?: "unknown")
            )
        )
    }

    private fun generateTemporaryPassword(): String {
        /*val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%"
        return (1..12).map { chars.random() }.joinToString("")*/
        return "Azerty@78"
    }
}