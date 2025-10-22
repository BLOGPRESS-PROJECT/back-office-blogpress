package com.kobe.blogpress_api.controller.user

import com.kobe.blogpress_api.dto.common.ApiResponseDto
import com.kobe.blogpress_api.dto.user.UpdateProfileRequestDTO
import com.kobe.blogpress_api.dto.user.UserDTO
import com.kobe.blogpress_api.services.fileStorage.FileStorageService
import com.kobe.blogpress_api.services.user.UserService
import jakarta.validation.Valid
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.util.*

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
    private val fileStorageService: FileStorageService
) {

    private val logger = LoggerFactory.getLogger(UserController::class.java)

    @GetMapping("/me")
    fun getCurrentUser(
        @AuthenticationPrincipal userId: String
    ): Mono<ResponseEntity<ApiResponseDto<UserDTO>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get current user: $userId")

        return userService.findById(ObjectId(userId))
            .map { user ->
                ResponseEntity.ok(
                    ApiResponseDto.success(
                        data = userService.toDTO(user),
                        message = "User retrieved successfully",
                        requestId = requestId
                    )
                )
            }
    }

    @GetMapping("/profile/{userId}")
    fun getUserProfile(
        @PathVariable userId: String
    ): Mono<ResponseEntity<ApiResponseDto<UserDTO>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get user profile: $userId")

        return userService.findById(ObjectId(userId))
            .map { user ->
                ResponseEntity.ok(
                    ApiResponseDto.success(
                        data = userService.toDTO(user),
                        message = "User profile retrieved successfully",
                        requestId = requestId
                    )
                )
            }
    }

    @GetMapping("/username/{username}")
    fun getUserByUsername(
        @PathVariable username: String
    ): Mono<ResponseEntity<ApiResponseDto<UserDTO>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get user by username: $username")

        return userService.findByUsername(username)
            .map { user ->
                ResponseEntity.ok(
                    ApiResponseDto.success(
                        data = userService.toDTO(user),
                        message = "User retrieved successfully",
                        requestId = requestId
                    )
                )
            }
    }

    @PutMapping("/me")
    fun updateProfile(
        @AuthenticationPrincipal userId: String,
        @Valid @RequestBody updateRequest: UpdateProfileRequestDTO
    ): Mono<ResponseEntity<ApiResponseDto<UserDTO>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Update profile for user: $userId")

        return userService.updateProfile(ObjectId(userId), updateRequest)
            .map { user ->
                logger.info("[$requestId] Profile updated successfully for user: $userId")
                ResponseEntity.ok(
                    ApiResponseDto.success(
                        data = userService.toDTO(user),
                        message = "Profile updated successfully",
                        requestId = requestId
                    )
                )
            }
    }

    @PostMapping("/me/profile-picture", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadProfilePicture(
        @AuthenticationPrincipal userId: String,
        @RequestPart("file") file: FilePart
    ): Mono<ResponseEntity<ApiResponseDto<UserDTO>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Upload profile picture for user: $userId")

        return fileStorageService.storeProfilePicture(file, userId)
            .flatMap { fileUrl ->
                logger.info("[$requestId] File uploaded successfully: $fileUrl")
                userService.updateProfilePicture(ObjectId(userId), fileUrl)
            }
            .map { user ->
                logger.info("[$requestId] Profile picture updated successfully for user: $userId")
                ResponseEntity.ok(
                    ApiResponseDto.success(
                        data = userService.toDTO(user),
                        message = "Profile picture uploaded successfully",
                        requestId = requestId
                    )
                )
            }
    }

    @DeleteMapping("/me/profile-picture")
    fun deleteProfilePicture(
        @AuthenticationPrincipal userId: String
    ): Mono<ResponseEntity<ApiResponseDto<UserDTO>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Delete profile picture for user: $userId")

        return userService.findById(ObjectId(userId))
            .flatMap { user ->
                val oldPicture = user.profilePicture
                fileStorageService.deleteProfilePicture(oldPicture ?: "")
                    .then(userService.updateProfilePicture(ObjectId(userId), ""))
            }
            .map { user ->
                logger.info("[$requestId] Profile picture deleted successfully for user: $userId")
                ResponseEntity.ok(
                    ApiResponseDto.success(
                        data = userService.toDTO(user),
                        message = "Profile picture deleted successfully",
                        requestId = requestId
                    )
                )
            }
    }

    @PostMapping("/follow/{targetUserId}")
    fun followUser(
        @AuthenticationPrincipal userId: String,
        @PathVariable targetUserId: String
    ): Mono<ResponseEntity<ApiResponseDto<Map<String, UserDTO>>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] User $userId following $targetUserId")

        return userService.followUser(ObjectId(userId), ObjectId(targetUserId))
            .map { (follower, following) ->
                logger.info("[$requestId] User $userId now follows $targetUserId")
                ResponseEntity.ok(
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
    }

    @PostMapping("/unfollow/{targetUserId}")
    fun unfollowUser(
        @AuthenticationPrincipal userId: String,
        @PathVariable targetUserId: String
    ): Mono<ResponseEntity<ApiResponseDto<Map<String, UserDTO>>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] User $userId unfollowing $targetUserId")

        return userService.unfollowUser(ObjectId(userId), ObjectId(targetUserId))
            .map { (follower, following) ->
                logger.info("[$requestId] User $userId unfollowed $targetUserId")
                ResponseEntity.ok(
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
}