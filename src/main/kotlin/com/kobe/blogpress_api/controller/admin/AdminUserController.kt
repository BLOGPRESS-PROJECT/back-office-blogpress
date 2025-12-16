package com.kobe.blogpress_api.controller.admin

import com.kobe.blogpress_api.dto.common.ApiResponseDto
import com.kobe.blogpress_api.dto.user.AdminUserListItemDTO
import com.kobe.blogpress_api.dto.user.BatchCreateUsersRequestDTO
import com.kobe.blogpress_api.dto.user.UserDTO
import com.kobe.blogpress_api.services.storage.StorageQuotaService
import com.kobe.blogpress_api.services.user.UserService
import jakarta.validation.Valid
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/admin/users")
class AdminUserController(
    private val userService: UserService,
    private val storageQuotaService: StorageQuotaService
) {

    private val logger = LoggerFactory.getLogger(AdminUserController::class.java)

    /**
     * Liste paginée des utilisateurs, avec filtres avancés.
     *
     * GET /api/admin/users?page=0&size=20&search=&role=&isGolden=&isActive=
     */
    @GetMapping
    suspend fun listUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) role: String?,       // "ADMIN", "USER", "MODERATOR"
        @RequestParam(required = false) isGolden: Boolean?,  // true / false / null
        @RequestParam(required = false) isActive: Boolean?   // true / false / null
    ): ResponseEntity<ApiResponseDto<Page<AdminUserListItemDTO>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Admin list users - page=$page, size=$size, search=$search, role=$role, isGolden=$isGolden, isActive=$isActive")

        val usersPage = userService.findAllUsers(
            page = page,
            size = size,
            search = search,
            role = role,
            isGolden = isGolden,
            isActive = isActive
        )

        // Mapper en AdminUserListItemDTO (avec stats à jour) via coroutines
        val dtoContent = coroutineScope {
            usersPage.content.map { user ->
                async { userService.toAdminListItemDTO(user) }
            }.awaitAll()
        }

        val dtoPage: Page<AdminUserListItemDTO> = PageImpl(
            dtoContent,
            usersPage.pageable,
            usersPage.totalElements
        )

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = dtoPage,
                message = "Users retrieved successfully",
                requestId = requestId
            )
        )
    }

    /**
     * Promotion d'un utilisateur en Golden User par un admin.
     *
     * POST /api/admin/users/{userId}/promote-golden
     */
    @PostMapping("/{userId}/promote-golden")
    suspend fun promoteToGoldenUser(
        @AuthenticationPrincipal adminId: String,
        @PathVariable userId: String
    ): ResponseEntity<ApiResponseDto<Any>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Admin $adminId promote user $userId to Golden")

        val user = userService.promoteToGoldenUser(ObjectId(userId), ObjectId(adminId))
        
        // ⭐ Mettre à jour le quota de stockage pour qu'il soit illimité
        try {
            storageQuotaService.updateQuotaToUnlimited(ObjectId(userId))
            logger.info("[$requestId] Storage quota updated to unlimited for Golden User: $userId")
        } catch (e: Exception) {
            logger.warn("[$requestId] Could not update storage quota for Golden User $userId: ${e.message}")
            // Ne pas faire échouer la promotion si le quota échoue
        }
        
        val dto = userService.toDTO(user)

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = dto,
                message = "User promoted to Golden User successfully",
                requestId = requestId
            )
        )
    }

    /**
     * Révocation du statut Golden User par un admin.
     *
     * POST /api/admin/users/{userId}/revoke-golden
     */
    @PostMapping("/{userId}/revoke-golden")
    suspend fun revokeGoldenUser(
        @AuthenticationPrincipal adminId: String,
        @PathVariable userId: String
    ): ResponseEntity<ApiResponseDto<Any>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Admin $adminId revoke Golden status for user $userId")

        val user = userService.revokeGoldenUser(ObjectId(userId), ObjectId(adminId))
        
        // ⭐ Mettre à jour le quota de stockage pour revenir à la limite standard
        // Note: Le quota existant reste, mais les nouveaux uploads seront limités
        // On pourrait aussi réinitialiser le quota ici si nécessaire
        
        val dto = userService.toDTO(user)

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = dto,
                message = "Golden User status revoked successfully",
                requestId = requestId
            )
        )
    }

    /**
     * Désactiver un utilisateur.
     *
     * POST /api/admin/users/{userId}/deactivate
     */
    @PostMapping("/{userId}/deactivate")
    suspend fun deactivateUser(
        @AuthenticationPrincipal adminId: String,
        @PathVariable userId: String
    ): ResponseEntity<ApiResponseDto<UserDTO>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Admin $adminId deactivate user $userId")

        val updated = userService.deactivateUser(ObjectId(userId))
        val dto = userService.toDTO(updated)

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = dto,
                message = "User deactivated successfully",
                requestId = requestId
            )
        )
    }

    /**
     * Activer un utilisateur.
     *
     * POST /api/admin/users/{userId}/activate
     */
    @PostMapping("/{userId}/activate")
    suspend fun activateUser(
        @AuthenticationPrincipal adminId: String,
        @PathVariable userId: String
    ): ResponseEntity<ApiResponseDto<UserDTO>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Admin $adminId activate user $userId")

        val updated = userService.activateUser(ObjectId(userId))
        val dto = userService.toDTO(updated)

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = dto,
                message = "User activated successfully",
                requestId = requestId
            )
        )
    }

    /**
     * Supprimer un utilisateur.
     *
     * DELETE /api/admin/users/{userId}
     */
    @DeleteMapping("/{userId}")
    suspend fun deleteUser(
        @AuthenticationPrincipal adminId: String,
        @PathVariable userId: String
    ): ResponseEntity<ApiResponseDto<Unit>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Admin $adminId delete user $userId")

        userService.deleteUser(ObjectId(userId))

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = Unit,
                message = "User deleted successfully",
                requestId = requestId
            )
        )
    }

    /**
     * Créer plusieurs utilisateurs en batch (pour les tests).
     *
     * POST /api/admin/users/batch-create
     */
    @PostMapping("/batch-create")
    suspend fun batchCreateUsers(
        @AuthenticationPrincipal adminId: String,
        @Valid @RequestBody request: BatchCreateUsersRequestDTO
    ): ResponseEntity<ApiResponseDto<Map<String, Any>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Admin $adminId creating ${request.count} users in batch")

        val result = userService.batchCreateUsers(request)

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = result,
                message = "${request.count} users created successfully",
                requestId = requestId
            )
        )
    }
}


