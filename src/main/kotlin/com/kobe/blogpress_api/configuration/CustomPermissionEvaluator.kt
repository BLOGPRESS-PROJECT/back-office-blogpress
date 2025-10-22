package com.kobe.blogpress_api.configuration

import com.kobe.blogpress_api.model.user.Permission
import com.kobe.blogpress_api.model.user.RolePermissionConfig
import com.kobe.blogpress_api.repository.user.UserRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.core.Authentication
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import java.io.Serializable

@Component
class CustomPermissionEvaluator(
    private val userRepository: UserRepository
) : PermissionEvaluator {

    override fun hasPermission(
        authentication: Authentication,
        targetDomainObject: Any?,
        permission: Any
    ): Boolean {
        println("=== DEBUG CustomPermissionEvaluator ===")
        println("Authentication: $authentication")
        println("Principal: ${authentication.principal}")
        println("Principal type: ${authentication.principal?.javaClass}")
        println("Permission: $permission")

        val userId = authentication.principal as? String ?: return false
        val permissionName = permission.toString()

        println("UserId extracted: $userId")
        println("Permission name: $permissionName")

        return try {
            val user = userRepository.findById(ObjectId(userId)).orElse(null) ?: return false
            val permissionEnum = Permission.valueOf(permissionName)
            RolePermissionConfig.hasPermission(user.roleType, permissionEnum)
        } catch (e: Exception) {
            false
        }
    }

    override fun hasPermission(
        authentication: Authentication,
        targetId: Serializable,
        targetType: String,
        permission: Any
    ): Boolean {
        return hasPermission(authentication, null, permission)
    }
}

// Gestionnaire d'accès refusé personnalisé
class CustomAccessDeniedHandler : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException?
    ) {
        response.status = HttpStatus.FORBIDDEN.value()
        response.contentType = "application/json"
        response.writer.write(
            """{"error": "Accès refusé", "message": "Vous n'avez pas les permissions nécessaires pour effectuer cette action"}"""
        )
    }
}