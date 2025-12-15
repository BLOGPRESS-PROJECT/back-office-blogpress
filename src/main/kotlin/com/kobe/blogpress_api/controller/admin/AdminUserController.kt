package com.kobe.blogpress_api.controller.admin

import com.kobe.blogpress_api.dto.common.ApiResponseDto
import com.kobe.blogpress_api.dto.user.UserDTO
import com.kobe.blogpress_api.services.user.UserService
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/admin/users")
class AdminUserController(
    private val userService: UserService
) {

    private val logger = LoggerFactory.getLogger(AdminUserController::class.java)

    /**
     * Liste paginée des utilisateurs, avec option de recherche.
     *
     * GET /api/admin/users?page=0&size=20&search=...
     */
    @GetMapping
    suspend fun listUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<ApiResponseDto<Page<UserDTO>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Admin list users - page=$page, size=$size, search=$search")

        // Si search est vide, on renvoie simplement tous les utilisateurs paginés
        val usersPage = if (search.isNullOrBlank()) {
            userService.findAllUsers(page, size)
        } else {
            userService.searchUsers(search, page, size)
        }

        // Mapper en DTO avec stats à jour
        val userDTOs = usersPage.content
            .asFlow()
            .map { userService.toDTO(it) }
            .toList()

        val pageDTOs: Page<UserDTO> = PageImpl(
            userDTOs,
            usersPage.pageable,
            usersPage.totalElements
        )

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = pageDTOs,
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
    ): ResponseEntity<ApiResponseDto<UserDTO>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Admin $adminId promote user $userId to Golden")

        val user = userService.promoteToGoldenUser(ObjectId(userId), ObjectId(adminId))

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = userService.toDTO(user),
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
    ): ResponseEntity<ApiResponseDto<UserDTO>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Admin $adminId revoke Golden status for user $userId")

        val user = userService.revokeGoldenUser(ObjectId(userId), ObjectId(adminId))

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = userService.toDTO(user),
                message = "Golden User status revoked successfully",
                requestId = requestId
            )
        )
    }
}


