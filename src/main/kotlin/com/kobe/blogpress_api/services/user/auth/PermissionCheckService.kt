package com.kobe.blogpress_api.services.user.auth

import com.kobe.blogpress_api.model.user.Permission
import com.kobe.blogpress_api.model.user.RolePermissionConfig
import com.kobe.blogpress_api.repository.user.UserRepository
import org.bson.types.ObjectId
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service


@Service
class PermissionCheckService(
    private val userRepository: UserRepository
) {
    fun hasPermission(authentication: Authentication, permissionName: String): Boolean {
        val userId = authentication.principal as? String ?: return false

        return try {
            val permission = Permission.valueOf(permissionName)
            val user = userRepository.findById(ObjectId(userId)).orElse(null) ?: return false
            RolePermissionConfig.hasPermission(user.roleType, permission)
        } catch (e: IllegalArgumentException) {
            false // Si la permission n'existe pas
        }
    }
}