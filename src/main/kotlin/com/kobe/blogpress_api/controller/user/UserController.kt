package com.kobe.blogpress_api.controller.user

import com.kobe.blogpress_api.domain.model.user.UserStatistics
import com.kobe.blogpress_api.dto.common.ApiResponseDto
import com.kobe.blogpress_api.dto.user.PrivacyPreferencesDTO
import com.kobe.blogpress_api.dto.user.PublicUserDTO
import com.kobe.blogpress_api.dto.user.UpdateProfileRequestDTO
import com.kobe.blogpress_api.dto.user.UserDTO
import com.kobe.blogpress_api.exception.ResourceNotFoundException
import com.kobe.blogpress_api.services.fileStorage.FileStorageService
import com.kobe.blogpress_api.services.user.UserService
import jakarta.validation.Valid
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
    private val fileStorageService: FileStorageService
) {

    private val logger = LoggerFactory.getLogger(UserController::class.java)

    @GetMapping("/me")
    suspend fun getCurrentUser(
        @AuthenticationPrincipal userId: String?
    ): ResponseEntity<ApiResponseDto<UserDTO>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get current user: $userId")

        if (userId.isNullOrBlank()) {
            logger.error("[$requestId] User ID is null or blank")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponseDto.error(
                    message = "Authentification requise",
                    requestId = requestId
                ))
        }

        try {
            val user = userService.findById(ObjectId(userId))
            return ResponseEntity.ok(
                ApiResponseDto.success(
                    data = userService.toDTO(user),
                    message = "User retrieved successfully",
                    requestId = requestId
                )
            )
        } catch (e: Exception) {
            logger.error("[$requestId] Error retrieving user", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error(
                    message = "Erreur lors de la récupération de l'utilisateur: ${e.message}",
                    requestId = requestId
                ))
        }
    }

    @GetMapping("/{userId}")
    suspend fun getUserById(
        @PathVariable userId: String
    ): ResponseEntity<ApiResponseDto<PublicUserDTO>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get user by ID: $userId")

        try {
            if (!ObjectId.isValid(userId)) {
                logger.warn("[$requestId] Invalid user ID format: $userId")
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDto.error(
                        message = "Format d'ID utilisateur invalide",
                        requestId = requestId
                    ))
            }

            val user = userService.findById(ObjectId(userId))
            val publicProfile = PublicUserDTO(
                id = user.id.toHexString(),
                username = user.username,
                fullName = "${user.firstName} ${user.lastName}",
                profilePicture = user.profilePicture,
                bio = user.bio,
                isGoldenUser = user.isGoldenUser,
                statistics = user.statistics
            )

            return ResponseEntity.ok(
                ApiResponseDto.success(
                    data = publicProfile,
                    message = "Profil utilisateur récupéré avec succès",
                    requestId = requestId
                )
            )
        } catch (e: ResourceNotFoundException) {
            logger.warn("[$requestId] User not found: $userId")
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.error(
                    message = "Utilisateur non trouvé",
                    requestId = requestId
                ))
        } catch (e: Exception) {
            logger.error("[$requestId] Error retrieving user: $userId", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error(
                    message = "Erreur lors de la récupération de l'utilisateur",
                    requestId = requestId
                ))
        }
    }

    @GetMapping("/profile/{userId}")
    suspend fun getUserProfile(
        @PathVariable userId: String
    ): ResponseEntity<ApiResponseDto<UserDTO>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get user profile: $userId")

        val user = userService.findById(ObjectId(userId))

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = userService.toDTO(user),
                message = "User profile retrieved successfully",
                requestId = requestId
            )
        )
    }

    @GetMapping("/username/{username}")
    suspend fun getUserByUsername(
        @PathVariable username: String
    ): ResponseEntity<ApiResponseDto<UserDTO>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get user by username: $username")

        val user = userService.findByUsername(username)

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = userService.toDTO(user),
                message = "User retrieved successfully",
                requestId = requestId
            )
        )
    }

    @PutMapping("/me")
    suspend fun updateProfile(
        @AuthenticationPrincipal userId: String,
        @Valid @RequestBody updateRequest: UpdateProfileRequestDTO
    ): ResponseEntity<ApiResponseDto<UserDTO>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Update profile for user: $userId")

        val user = userService.updateProfile(ObjectId(userId), updateRequest)

        logger.info("[$requestId] Profile updated successfully for user: $userId")
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = userService.toDTO(user),
                message = "Profile updated successfully",
                requestId = requestId
            )
        )
    }

    @PostMapping("/me/profile-picture", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun uploadProfilePicture(
        @AuthenticationPrincipal userId: String,
        @RequestPart("file") filePart: FilePart
    ): ResponseEntity<ApiResponseDto<UserDTO>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Upload profile picture for user: $userId")

        // Appel direct sans .awaitSingle() car c'est déjà une suspend function
        val fileUrl = fileStorageService.storeProfilePicture(filePart, userId)
        logger.info("[$requestId] File uploaded successfully: $fileUrl")

        val user = userService.updateProfilePicture(ObjectId(userId), fileUrl)

        logger.info("[$requestId] Profile picture updated successfully for user: $userId")
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = userService.toDTO(user),
                message = "Profile picture uploaded successfully",
                requestId = requestId
            )
        )
    }

    @DeleteMapping("/me/profile-picture")
    suspend fun deleteProfilePicture(
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<ApiResponseDto<UserDTO>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Delete profile picture for user: $userId")

        val user = userService.findById(ObjectId(userId))
        val oldPicture = user.profilePicture

        // Appel direct sans .awaitSingle()
        fileStorageService.deleteProfilePicture(oldPicture ?: "")
        val updatedUser = userService.updateProfilePicture(ObjectId(userId), "")

        logger.info("[$requestId] Profile picture deleted successfully for user: $userId")
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = userService.toDTO(updatedUser),
                message = "Profile picture deleted successfully",
                requestId = requestId
            )
        )
    }

    // Dans UserController.kt
    @GetMapping("/me/following/{targetUserId}")
    suspend fun isFollowing(
        @AuthenticationPrincipal userId: String,
        @PathVariable targetUserId: String
    ): ResponseEntity<ApiResponseDto<Boolean>> {
        val isFollowing = userService.isFollowing(ObjectId(userId), ObjectId(targetUserId))
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = isFollowing,
                message = "Follow status retrieved",
                requestId = UUID.randomUUID().toString()
            )
        )
    }

    @PostMapping("/follow/{targetUserId}")
    suspend fun followUser(
        @AuthenticationPrincipal userId: String,
        @PathVariable targetUserId: String
    ): ResponseEntity<ApiResponseDto<Map<String, UserDTO>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] User $userId following $targetUserId")

        val (follower, following) = userService.followUser(ObjectId(userId), ObjectId(targetUserId))

        logger.info("[$requestId] User $userId now follows $targetUserId")
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = mapOf(
                    "follower" to userService.toDTO(follower),
                    "following" to userService.toDTO(following)
                ),
                message = "Successfully followed user",
                requestId = requestId
            )
        )
    }

    @PostMapping("/unfollow/{targetUserId}")
    suspend fun unfollowUser(
        @AuthenticationPrincipal userId: String,
        @PathVariable targetUserId: String
    ): ResponseEntity<ApiResponseDto<Map<String, UserDTO>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] User $userId unfollowing $targetUserId")

        val (follower, following) = userService.unfollowUser(ObjectId(userId), ObjectId(targetUserId))

        logger.info("[$requestId] User $userId unfollowed $targetUserId")
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = mapOf(
                    "follower" to userService.toDTO(follower),
                    "following" to userService.toDTO(following)
                ),
                message = "Successfully unfollowed user",
                requestId = requestId
            )
        )
    }

    @PostMapping("/{userId}/promote-golden")
    suspend fun promoteToGoldenUser(
        @AuthenticationPrincipal adminId: String,
        @PathVariable userId: String
    ): ResponseEntity<ApiResponseDto<UserDTO>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Promote user $userId to Golden by admin $adminId")

        val user = userService.promoteToGoldenUser(ObjectId(userId), ObjectId(adminId))

        logger.info("[$requestId] User $userId promoted to Golden User")
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = userService.toDTO(user),
                message = "User promoted to Golden User successfully",
                requestId = requestId
            )
        )
    }

    @PostMapping("/{userId}/revoke-golden")
    suspend fun revokeGoldenUser(
        @AuthenticationPrincipal adminId: String,
        @PathVariable userId: String
    ): ResponseEntity<ApiResponseDto<UserDTO>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Revoke Golden status for user $userId by admin $adminId")

        val user = userService.revokeGoldenUser(ObjectId(userId), ObjectId(adminId))

        logger.info("[$requestId] Golden status revoked for user $userId")
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = userService.toDTO(user),
                message = "Golden User status revoked successfully",
                requestId = requestId
            )
        )
    }

    @GetMapping("/search")
    suspend fun searchUsers(
        @RequestParam query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponseDto<Page<UserDTO>>> {
        val users = userService.searchUsers(query, page, size)
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = users.map { userService.toDTO(it) },
                message = "Users found",
                requestId = UUID.randomUUID().toString()
            )
        )
    }

    @GetMapping("/{userId}/public")
    suspend fun getPublicProfile(
        @PathVariable userId: String
    ): ResponseEntity<ApiResponseDto<PublicUserDTO>> {
        val user = userService.findById(ObjectId(userId))
        val publicProfile = PublicUserDTO(
            id = user.id.toHexString(),
            username = user.username,
            fullName = "${user.firstName} ${user.lastName}",
            profilePicture = user.profilePicture,
            bio = user.bio,
            isGoldenUser = user.isGoldenUser,
            statistics = user.statistics
        )
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = publicProfile,
                message = "Public profile retrieved",
                requestId = UUID.randomUUID().toString()
            )
        )
    }

    @GetMapping("/me/statistics")
    suspend fun getMyStatistics(
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<ApiResponseDto<UserStatistics>> {
        val user = userService.findById(ObjectId(userId))
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = user.statistics,
                message = "Statistics retrieved",
                requestId = UUID.randomUUID().toString()
            )
        )
    }

    @PutMapping("/me/privacy")
    suspend fun updatePrivacyPreferences(
        @AuthenticationPrincipal userId: String,
        @Valid @RequestBody preferences: PrivacyPreferencesDTO
    ): ResponseEntity<ApiResponseDto<UserDTO>> {
        val requestId = UUID.randomUUID().toString()

        return try {
            val user = userService.updatePrivacyPreferences(ObjectId(userId), preferences)
            val userDTO = userService.toDTO(user)

            ResponseEntity.ok(
                ApiResponseDto.success(
                    data = userDTO,
                    message = "Privacy preferences updated successfully",
                    requestId = requestId
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error<UserDTO>(
                    message = "Error updating privacy preferences: ${e.message}",
                    errorCode = "UPDATE_PRIVACY_ERROR",
                    errorDetails = mapOf("exception" to (e.message ?: "Unknown error")),
                    requestId = requestId
                ))
        }
    }
}