package com.kobe.blogpress_api.controller.images

import com.kobe.blogpress_api.dto.common.ApiResponseDto
import com.kobe.blogpress_api.services.fileStorage.UserImageService
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.nio.file.Files
import java.time.Instant
import java.util.*

/**
 * Controller pour servir les images de profil des utilisateurs
 *
 * NOTE: Les fichiers sont déjà servis statiquement via /uploads/ grâce à FileStorageConfig.
 * Ce controller est OPTIONNEL. Vous pouvez l'utiliser si vous voulez :
 * - Un endpoint API dédié avec des headers de cache personnalisés
 * - Un meilleur contrôle sur les erreurs (404, 500)
 * - Des logs détaillés
 * - Des métadonnées sur les images via ApiResponseDto
 *
 * Si vous n'en avez pas besoin, vous pouvez supprimer ce controller et utiliser
 * directement les URLs /uploads/profile-pictures/{filename}
 */
@RestController
@RequestMapping("/api/users")
class UserImageController(
    private val userImageService: UserImageService
) {

    private val logger = LoggerFactory.getLogger(UserImageController::class.java)

    /**
     * DTO pour les métadonnées d'une image de profil
     */
    data class ProfilePictureMetadata(
        val userId: String,
        val filename: String?,
        val url: String,
        val exists: Boolean,
        val size: Long? = null,
        val contentType: String? = null
    )

    /**
     * Récupérer la photo de profil d'un utilisateur par son ID (fichier binaire)
     * GET /api/users/{userId}/profile-picture
     */
    @GetMapping("/{userId}/profile-picture")
    suspend fun getProfilePicture(
        @PathVariable userId: String
    ): ResponseEntity<Resource> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get profile picture for user: $userId")

        return runBlocking {
            try {
                val resource = userImageService.getProfilePictureResource(userId)

                if (resource == null) {
                    logger.warn("[$requestId] Profile picture not found for user: $userId")
                    return@runBlocking ResponseEntity.notFound().build()
                }

                val file = resource.file
                val contentType = Files.probeContentType(file.toPath())
                    ?: "application/octet-stream"

                logger.info("[$requestId] Profile picture found for user: $userId, contentType: $contentType")

                ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${file.name}\"")
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600") // Cache 1 heure
                    .header("X-Request-Id", requestId)
                    .body(resource)
            } catch (e: Exception) {
                logger.error("[$requestId] Error serving profile picture for user $userId", e)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Récupérer les métadonnées de la photo de profil d'un utilisateur
     * GET /api/users/{userId}/profile-picture/metadata
     */
    @GetMapping("/{userId}/profile-picture/metadata")
    suspend fun getProfilePictureMetadata(
        @PathVariable userId: String
    ): ResponseEntity<ApiResponseDto<ProfilePictureMetadata>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get profile picture metadata for user: $userId")

        return runBlocking {
            try {
                val resource = userImageService.getProfilePictureResource(userId)

                if (resource == null || !resource.exists()) {
                    val metadata = ProfilePictureMetadata(
                        userId = userId,
                        filename = null,
                        url = "/api/users/$userId/profile-picture",
                        exists = false
                    )

                    return@runBlocking ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponseDto.error<ProfilePictureMetadata>(
                            message = "Profile picture not found for user: $userId",
                            errorCode = "PROFILE_PICTURE_NOT_FOUND",
                            errorDetails = mapOf("userId" to userId),
                            requestId = requestId
                        ))
                }

                val file = resource.file
                val filename = file.name
                val size = if (Files.exists(file.toPath())) Files.size(file.toPath()) else null
                val contentType = Files.probeContentType(file.toPath())

                val metadata = ProfilePictureMetadata(
                    userId = userId,
                    filename = filename,
                    url = "/api/users/$userId/profile-picture",
                    exists = true,
                    size = size,
                    contentType = contentType
                )

                logger.info("[$requestId] Profile picture metadata retrieved for user: $userId")

                ResponseEntity.ok(
                    ApiResponseDto.success(
                        data = metadata,
                        message = "Profile picture metadata retrieved successfully",
                        requestId = requestId,
                        metadata = mapOf(
                            "contentType" to (contentType ?: "unknown"),
                            "sizeBytes" to (size ?: 0)
                        )
                    )
                )
            } catch (e: Exception) {
                logger.error("[$requestId] Error retrieving profile picture metadata for user $userId", e)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDto.error<ProfilePictureMetadata>(
                        message = "Error retrieving profile picture metadata: ${e.message}",
                        errorCode = "INTERNAL_SERVER_ERROR",
                        errorDetails = mapOf("exception" to (e.message ?: "Unknown error")),
                        requestId = requestId
                    ))
            }
        }
    }

    /**
     * Récupérer la photo de profil par nom de fichier (fichier binaire)
     * GET /api/users/profile-pictures/{filename}
     *
     * Exemple: /api/users/profile-pictures/user123_abc123.jpg
     */
    @GetMapping("/profile-pictures/{filename:.+}")
    suspend fun getProfilePictureByFilename(
        @PathVariable filename: String
    ): ResponseEntity<Resource> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get profile picture by filename: $filename")

        return runBlocking {
            try {
                val resource = userImageService.getProfilePictureResourceByFilename(filename)

                if (resource == null) {
                    logger.warn("[$requestId] Profile picture not found: $filename")
                    return@runBlocking ResponseEntity.notFound().build()
                }

                val file = resource.file
                val contentType = Files.probeContentType(file.toPath())
                    ?: "application/octet-stream"

                logger.info("[$requestId] Profile picture found: $filename, contentType: $contentType")

                ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"$filename\"")
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600") // Cache 1 heure
                    .header("X-Request-Id", requestId)
                    .body(resource)
            } catch (e: Exception) {
                logger.error("[$requestId] Error serving profile picture: $filename", e)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Vérifier si un utilisateur a une photo de profil
     * GET /api/users/{userId}/profile-picture/exists
     */
    @GetMapping("/{userId}/profile-picture/exists")
    suspend fun checkProfilePictureExists(
        @PathVariable userId: String
    ): ResponseEntity<ApiResponseDto<Map<String, Any>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Check profile picture existence for user: $userId")

        return runBlocking {
            try {
                val exists = userImageService.profilePictureExists(userId)

                // Spécifier explicitement le type de la map
                val responseData = mapOf<String, Any>(
                    "userId" to userId,
                    "exists" to exists,
                    ("url" to if (exists) "/api/users/$userId/profile-picture" else null) as Pair<String, Any>
                )

                logger.info("[$requestId] Profile picture exists check for user $userId: $exists")

                ResponseEntity.ok(
                    ApiResponseDto.success(
                        data = responseData,
                        message = if (exists) "Profile picture exists" else "Profile picture not found",
                        requestId = requestId
                    )
                )
            } catch (e: Exception) {
                logger.error("[$requestId] Error checking profile picture existence for user $userId", e)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDto.error<Map<String, Any>>(
                        message = "Error checking profile picture existence: ${e.message}",
                        errorCode = "INTERNAL_SERVER_ERROR",
                        errorDetails = mapOf("exception" to (e.message ?: "Unknown error")),
                        requestId = requestId
                    ))
            }
        }
    }
}