package com.kobe.blogpress_api.services.user

import com.kobe.blogpress_api.configuration.HashEncoder
import com.kobe.blogpress_api.dto.user.CreateUserRequestDto
import com.kobe.blogpress_api.dto.user.RequestStatus
import com.kobe.blogpress_api.dto.user.RequestType
import com.kobe.blogpress_api.dto.user.ReviewRequestDto
import com.kobe.blogpress_api.dto.user.UpdateUserRequest
import com.kobe.blogpress_api.dto.user.UserRequest
import com.kobe.blogpress_api.dto.user.UserRequestSummary
import com.kobe.blogpress_api.model.user.User
import com.kobe.blogpress_api.repository.user.UserRepository
import com.kobe.blogpress_api.repository.user.UserRequestRepository
import com.kobe.blogpress_api.services.audit.AuditService
import com.kobe.blogpress_api.services.user.auth.AuthService
import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@Service
class UserManagementService (
    private val userRepository: UserRepository,
    private val userRequestRepository: UserRequestRepository,
    private val auditService: AuditService,
    private val hashEncoder: HashEncoder,
    private val authService: AuthService
) {

    /**
     * Crée une demande de reset de mot de passe (utilisateur non connecté)
     */
    fun createPasswordResetRequest(email: String, motif: String): UserRequest {
        val user = userRepository.findByEmail(email) ?: throw IllegalArgumentException(
            "Aucun utilisateur trouvé avec cet email"
        )

        // Vérifier qu'il n'y a pas déjà une demande en attente
        val existingRequest = userRequestRepository.findByUserIdAndStatus(
            user.id, RequestStatus.PENDING
        ).find { it.requestType == RequestType.PASSWORD_RESET }

        if (existingRequest != null) {
            throw IllegalStateException("Une demande de réinitialisation est déjà en attente")
        }

        val request = UserRequest(
            userId = user.id,
            requestType = RequestType.PASSWORD_RESET,
            motif = motif,
            requestData = mapOf("email" to email),
            expiresAt = Instant.now().plusSeconds(7 * 24 * 60 * 60) // 7 jours
        )

        val savedRequest = userRequestRepository.save(request)

        auditService.log(
            userId = user.id,
            action = "CREATE_PASSWORD_RESET_REQUEST",
            resource = "UserRequest",
            resourceId = savedRequest.id.toHexString(),
            details = mapOf(
                "email" to email,
                "motif" to motif
            )
        )
        return savedRequest
    }

    /**
     * Met à jour un utilisateur
     */
    fun updateUserCredentials(
        userId: String,
        updateRequest: UpdateUserRequest,
        adminId: String
    ): User {
        val targetUser = userRepository.findById(ObjectId(userId)).orElseThrow {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé")
        }

        // Vérifier si l'utilisateur n'est pas définitivement désactivé
        if (targetUser.permanentlyDisabled) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Impossible de modifier un compte définitivement désactivé"
            )
        }


        // Vérifier si l'email est déjà utilisé (si changement d'email)
        if (updateRequest.email != null && updateRequest.email != targetUser.email) {
            val existingUser = userRepository.findByEmail(updateRequest.email)
            if (existingUser != null) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Cet email est déjà utilisé")
            }
        }

        // Préparer les nouvelles valeurs
        val newEmail = updateRequest.email ?: targetUser.email
        val newHashedPassword = if (updateRequest.newPassword != null) {
            hashEncoder.encode(updateRequest.newPassword)
        } else {
            targetUser.hashedPassword
        }
        val newFirstName = updateRequest.firstName ?: targetUser.firstName
        val newLastName = updateRequest.lastName ?: targetUser.lastName

        // Mettre à jour l'utilisateur
        val updatedUser = targetUser.copy(
            email = newEmail,
            hashedPassword = newHashedPassword,
            firstName = newFirstName,
            lastName = newLastName,
            updatedAt = Instant.now(),
            mustChangePassword = if (updateRequest.newPassword != null) true else targetUser.mustChangePassword,
            // Réinitialisation des tentatives de connexion si changement de mot de passe
            loginAttempts = if (updateRequest.newPassword != null) 0 else targetUser.loginAttempts,
            lastFailedLoginAttempt = if (updateRequest.newPassword != null) null else targetUser.lastFailedLoginAttempt,
            blockedUntil = if (updateRequest.newPassword != null) null else targetUser.blockedUntil
        )


        val savedUser = userRepository.save(updatedUser)

        // Audit des modifications
        val changes = mutableMapOf<String, Any>()
        if (updateRequest.email != null) changes["email"] = "changed"
        if (updateRequest.newPassword != null) changes["password"] = "changed"
        if (updateRequest.firstName != null) changes["firstName"] = "changed"
        if (updateRequest.lastName != null) changes["lastName"] = "changed"

        auditService.log(
            userId = ObjectId(adminId),
            action = "UPDATE_USER_CREDENTIALS",
            resource = "User",
            resourceId = userId,
            details = mapOf(
                "targetUser" to targetUser.email,
                "changes" to changes,
                "updatedBy" to adminId
            )
        )
        return savedUser
    }

    /**
     * Crée une nouvelle requête utilisateur
     */
    fun createUserRequest(
        userId: String,
        requestDto: CreateUserRequestDto
    ): UserRequest {
        val user = userRepository.findById(ObjectId(userId)).orElseThrow {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé")
        }

        // Nouvelle validation
        if (user.permanentlyDisabled) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Impossible de créer une requête pour un compte définitivement désactivé"
            )
        }

        // Vérification spécifique pour les requêtes d'activation
        if (requestDto.requestType == RequestType.ACCOUNT_ACTIVATION && user.isActive) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Le compte est déjà actif"
            )
        }

        // Vérifier s'il n'y a pas déjà une requête en attente du même type
        val existingRequest = userRequestRepository.findByUserIdAndStatus(
            ObjectId(userId),
            RequestStatus.PENDING
        ).find { it.requestType == requestDto.requestType }

        if (existingRequest != null) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Vous avez déjà une requête en attente pour ce type de demande"
            )
        }

        // Calculer la date d'expiration (30 jours par défaut)
        val expiresAt = Instant.now().plusSeconds(7 * 24 * 60 * 60) // 7 jours

        val request = UserRequest(
            userId = ObjectId(userId),
            requestType = requestDto.requestType,
            motif = requestDto.motif,
            requestData = requestDto.requestData,
            expiresAt = expiresAt
        )

        val savedRequest = userRequestRepository.save(request)

        // Audit de création de requête
        auditService.log(
            userId = ObjectId(userId),
            action = "CREATE_USER_REQUEST",
            resource = "UserRequest",
            resourceId = savedRequest.id.toHexString(),
            details = mapOf(
                "requestType" to requestDto.requestType.name,
                "motif" to requestDto.motif,
                "requestData" to requestDto.requestData
            )
        )
        return savedRequest
    }

    /**
     * Récupère toutes les requêtes en attente
     */
    fun getPendingRequests(pageable: Pageable): Page<UserRequestSummary> {
        val requests = userRequestRepository.findByStatus(RequestStatus.PENDING, pageable)
        return requests.map { request ->
            val user = userRepository.findById(request.userId).orElse(null)
            UserRequestSummary(
                id = request.id.toHexString(),
                userId = request.userId.toHexString(),
                userEmail = user?.email ?: "Utilisateur supprimé",
                userName = if (user != null) "${user.firstName} ${user.lastName}" else "Utilisateur supprimé",
                requestType = request.requestType,
                motif = request.motif,
                status = request.status,
                requestData = request.requestData,
                createdAt = request.createdAt,
                updatedAt = request.updatedAt,
                reviewedBy = request.reviewedBy?.toHexString(),
                reviewedAt = request.reviewedAt,
                reviewComment = request.reviewComment
            )
        }
    }

    /**
     * Traite une requête (approbation ou rejet)
     */
    fun reviewRequest(
        requestId: String,
        reviewDto: ReviewRequestDto,
        adminId: String
    ): UserRequest {
        val request = userRequestRepository.findById(ObjectId(requestId)).orElseThrow {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Requête non trouvée")
        }

        if (request.status != RequestStatus.PENDING) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette requête a déjà été traitée")
        }

        // Vérifier si la requête n'a pas expiré
        if (request.expiresAt != null && Instant.now().isAfter(request.expiresAt)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette requête a expiré")
        }

        val newStatus = if (reviewDto.approved) RequestStatus.APPROVED else RequestStatus.REJECTED
        val updatedRequest = request.copy(
            status = newStatus,
            reviewedBy = ObjectId(adminId),
            reviewedAt = Instant.now(),
            reviewComment = reviewDto.reviewComment,
            updatedAt = Instant.now()
        )

        val savedRequest = userRequestRepository.save(updatedRequest)

        // Si approuvée, exécuter l'action correspondante
        if (reviewDto.approved) {
            executeApprovedRequest(savedRequest, adminId)
        }

        // Audit de traitement de requête
        auditService.log(
            userId = ObjectId(adminId),
            action = if (reviewDto.approved) "APPROVE_REQUEST" else "REJECT_REQUEST",
            resource = "UserRequest",
            resourceId = requestId,
            details = mapOf(
                "requestType" to request.requestType.name,
                "requesterId" to request.userId.toHexString(),
                "reviewComment" to reviewDto.reviewComment,
                "originalMotif" to request.motif
            ) as Map<String, Any>
        )

        return savedRequest
    }

    /**
     * Exécute une requête approuvée
     */
    private fun executeApprovedRequest(request: UserRequest, adminId: String) {
        when (request.requestType) {
            RequestType.PASSWORD_RESET -> {
                // Pour les demandes de reset (utilisateur non connecté)
                // L'admin approuve et un mot de passe temporaire est généré
                val temporaryPassword = authService.resetPasswordByAdmin(
                    request.userId.toHexString(),
                    adminId
                )

                // TODO: Envoyer le mot de passe temporaire par email
                // mailService.sendPasswordReset(userEmail, temporaryPassword)
            }

            RequestType.PASSWORD_CHANGE -> {
                // Pour les demandes de changement de mot de passe (utilisateur connecté via requête)
                val newPassword = request.requestData["newPassword"] as? String
                if (newPassword != null) {
                    authService.resetPasswordByAdmin(request.userId.toHexString(), adminId)
                }
            }

            RequestType.EMAIL_CHANGE -> {
                val newEmail = request.requestData["newEmail"] as? String
                if (newEmail != null) {
                    updateUserCredentials(
                        request.userId.toHexString(),
                        UpdateUserRequest(email = newEmail),
                        adminId
                    )
                }
            }

            RequestType.PROFILE_UPDATE -> {
                val firstName = request.requestData["firstName"] as? String
                val lastName = request.requestData["lastName"] as? String
                updateUserCredentials(
                    request.userId.toHexString(),
                    UpdateUserRequest(firstName = firstName, lastName = lastName),
                    adminId
                )
            }

            RequestType.ACCOUNT_ACTIVATION -> {
                val user = userRepository.findById(request.userId).orElse(null)
                if (user != null && !user.isActive) {
                    val activatedUser = user.copy(
                        isActive = true,
                        updatedAt = Instant.now(),
                        deactivationReason = null,
                        deactivationComment = null,
                        deactivatedAt = null,
                        deactivatedBy = null,
                        blockedUntil = null,
                        loginAttempts = 0,
                        lastFailedLoginAttempt = null,
                        //temporaryBlockCount = 0
                    )
                    userRepository.save(activatedUser)

                    auditService.log(
                        userId = ObjectId(adminId),
                        action = "ACTIVATE_USER",
                        resource = "User",
                        resourceId = user.id.toHexString(),
                        details = mapOf(
                            "requestId" to request.id.toHexString(),
                            "previousState" to mapOf(
                                "deactivationReason" to user.deactivationReason?.name,
                                "deactivatedAt" to user.deactivatedAt?.toString(),
                                //"temporaryBlockCount" to user.temporaryBlockCount
                            )
                        )
                    )
                }
            }


            else -> {
                auditService.log(
                    userId = ObjectId(adminId),
                    action = "EXECUTE_REQUEST_NOT_IMPLEMENTED",
                    resource = "UserRequest",
                    resourceId = request.id.toHexString(),
                    details = mapOf("requestType" to request.requestType.name)
                )
            }
        }
    }

    /**
     * Annule une requête en attente
     */
    fun cancelUserRequest(requestId: String, userId: String): UserRequest {
        val request = userRequestRepository.findById(ObjectId(requestId)).orElseThrow {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Requête non trouvée")
        }

        if (request.userId.toHexString() != userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Vous ne pouvez annuler que vos propres requêtes")
        }

        if (request.status != RequestStatus.PENDING) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Seules les requêtes en attente peuvent être annulées")
        }

        val cancelledRequest = request.copy(
            status = RequestStatus.CANCELLED,
            updatedAt = Instant.now()
        )

        val savedRequest = userRequestRepository.save(cancelledRequest)

        // Audit d'annulation
        auditService.log(
            userId = ObjectId(userId),
            action = "CANCEL_USER_REQUEST",
            resource = "UserRequest",
            resourceId = requestId,
            details = mapOf(
                "requestType" to request.requestType.name,
                "originalMotif" to request.motif
            )
        )

        return savedRequest
    }

    /**
     * Récupère les statistiques des requêtes
     */
    fun getRequestStatistics(): Map<String, Any> {
        val totalRequests = userRequestRepository.count()
        val pendingCount = userRequestRepository.countByStatus(RequestStatus.PENDING)
        val approvedCount = userRequestRepository.countByStatus(RequestStatus.APPROVED)
        val rejectedCount = userRequestRepository.countByStatus(RequestStatus.REJECTED)
        val cancelledCount = userRequestRepository.countByStatus(RequestStatus.CANCELLED)

        val requestsByType = RequestType.values().associate { type ->
            type.name to userRequestRepository.countByRequestType(type)
        }

        return mapOf(
            "totalRequests" to totalRequests,
            "pendingCount" to pendingCount,
            "approvedCount" to approvedCount,
            "rejectedCount" to rejectedCount,
            "cancelledCount" to cancelledCount,
            "requestsByType" to requestsByType,
            "generatedAt" to Instant.now().toString()
        )
    }

    /**
     * Récupère les requêtes d'un utilisateur
     */
    fun getUserRequests(userId: String): List<UserRequestSummary> {
        val requests = userRequestRepository.findByUserId(ObjectId(userId))
        return requests.map { request ->
            val user = userRepository.findById(request.userId).orElse(null)
            UserRequestSummary(
                id = request.id.toHexString(),
                userId = request.userId.toHexString(),
                userEmail = user?.email ?: "Utilisateur supprimé",
                userName = if (user != null) "${user.firstName} ${user.lastName}" else "Utilisateur supprimé",
                requestType = request.requestType,
                motif = request.motif,
                status = request.status,
                requestData = request.requestData,
                createdAt = request.createdAt,
                updatedAt = request.updatedAt,
                reviewedBy = request.reviewedBy?.toHexString(),
                reviewedAt = request.reviewedAt,
                reviewComment = request.reviewComment
            )
        }
    }

}