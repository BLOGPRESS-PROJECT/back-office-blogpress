package com.kobe.blogpress_api.controller.user

import com.kobe.blogpress_api.dto.common.ApiResponseDto
import com.kobe.blogpress_api.dto.user.UpdateProfileRequestDTO
import com.kobe.blogpress_api.dto.user.UserDTO
import com.kobe.blogpress_api.services.fileStorage.FileStorageService
import com.kobe.blogpress_api.services.user.UserService
import jakarta.validation.Valid
import kotlinx.coroutines.reactor.awaitSingle
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
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
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<ApiResponseDto<UserDTO>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get current user: $userId")

        val user = userService.findById(ObjectId(userId))

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = userService.toDTO(user),
                message = "User retrieved successfully",
                requestId = requestId
            )
        )
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

        val fileUrl = fileStorageService.storeProfilePicture(filePart, userId).awaitSingle()
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

        fileStorageService.deleteProfilePicture(oldPicture ?: "").awaitSingle()
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
}