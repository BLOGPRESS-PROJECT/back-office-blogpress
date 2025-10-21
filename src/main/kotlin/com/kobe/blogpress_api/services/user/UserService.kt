package com.kobe.blogpress_api.services.user

import com.kobe.blogpress_api.model.user.User
import com.kobe.blogpress_api.repository.user.UserRepository
import com.kobe.blogpress_api.services.audit.AuditService
import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class UserService(
    private val userRepository: UserRepository,
    private val auditService: AuditService
) {

    /**
     * Trouve un utilisateur par son nom d'utilisateur (email)
     * @param username l'email de l'utilisateur
     * @return l'utilisateur trouvé
     * @throws UsernameNotFoundException si l'utilisateur n'est pas trouvé
     */
    fun findByUsername(username: String): User {
        return userRepository.findByEmail(username)
            ?: throw UsernameNotFoundException("Utilisateur non trouvé avec l'email : $username")
    }


    /**
     * Supprime définitivement un utilisateur (SYSTEM_ADMIN uniquement)
     */
    fun deleteUser(userId: String, adminId: String): Boolean {
        val targetUser = userRepository.findById(ObjectId(userId)).orElseThrow {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé")
        }
        // Vérifier que l'utilisateur n'est pas déjà supprimé
        if (!targetUser.isActive) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "L'utilisateur est déjà inactif")
        }
        // Supprimer l'utilisateur
        userRepository.deleteById(ObjectId(userId))

        // Audit de suppression
        auditService.log(
            userId = ObjectId(adminId),
            action = "DELETE_USER",
            resource = "User",
            resourceId = userId,
            details = mapOf(
                "targetUser" to targetUser.email,
                "targetUserName" to "${targetUser.firstName} ${targetUser.lastName}",
                "targetUserRole" to targetUser.roleType.name,
                "targetUserDepartment" to targetUser.department,
                "deletionType" to "PERMANENT_DELETE"
            )
        )
        return true
    }

    /**
     * Récupère tous les utilisateurs (actifs et inactifs) pour l'admin système et les directeurs
     */
    fun getAllUsersWithInactive(pageable: Pageable): Page<User> {
        return userRepository.findAll(pageable)
    }

    /**
     * Trouve un utilisateur par son ID
     * @param id l'ID de l'utilisateur
     * @return l'utilisateur trouvé
     * @throws EntityNotFoundException si l'utilisateur n'est pas trouvé
     */
    fun findById(id: ObjectId): User {
        return userRepository.findById(id).orElseThrow {
            Exception("Utilisateur non trouvé avec l'ID : ${id.toHexString()}")
        }
    }

}