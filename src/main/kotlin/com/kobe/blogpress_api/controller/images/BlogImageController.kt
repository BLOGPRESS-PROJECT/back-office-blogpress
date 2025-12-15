package com.kobe.blogpress_api.controller.images

import com.kobe.blogpress_api.dto.blog.UpdateBlogRequest
import com.kobe.blogpress_api.dto.common.ApiResponseDto
import com.kobe.blogpress_api.services.blog.BlogService
import com.kobe.blogpress_api.services.fileStorage.BlogImageService
import com.kobe.blogpress_api.services.fileStorage.FileStorageService
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.nio.file.Files
import java.util.UUID

@RestController
@RequestMapping("/api/blogs")
class BlogImageController(
    private val blogService: BlogService,
    private val fileStorageService: FileStorageService,
    private val blogImageService: BlogImageService
) {

    private val logger = LoggerFactory.getLogger(BlogImageController::class.java)
    
    /**
     * Récupérer l'image de couverture d'un blog (fichier binaire)
     * GET /api/blogs/{blogId}/cover-image
     */
    @GetMapping("/{blogId}/cover-image")
    suspend fun getBlogCoverImage(
        @PathVariable blogId: String
    ): ResponseEntity<Resource> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get cover image for blog: $blogId")

        return runBlocking {
            try {
                val resource = blogImageService.getCoverImageResource(blogId)
                if (resource == null || !resource.exists()) {
                    logger.warn("[$requestId] Cover image not found for blog: $blogId")
                    return@runBlocking ResponseEntity.notFound().build()
                }

                val file = resource.file
                val contentType = Files.probeContentType(file.toPath())
                    ?: "application/octet-stream"

                logger.info("[$requestId] Cover image found for blog: $blogId, contentType: $contentType")

                ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${file.name}\"")
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600") // Cache 1 heure
                    .header("X-Request-Id", requestId)
                    .header("ngrok-skip-browser-warning", "true") // Pour ngrok
                    .body(resource)
            } catch (e: Exception) {
                logger.error("[$requestId] Error serving cover image for blog $blogId", e)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Récupérer l'image logo d'un blog (fichier binaire)
     * GET /api/blogs/{blogId}/logo-image
     */
    @GetMapping("/{blogId}/logo-image")
    suspend fun getBlogLogoImage(
        @PathVariable blogId: String
    ): ResponseEntity<Resource> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get logo image for blog: $blogId")

        return runBlocking {
            try {
                val resource = blogImageService.getLogoImageResource(blogId)
                if (resource == null || !resource.exists()) {
                    logger.warn("[$requestId] Logo image not found for blog: $blogId")
                    return@runBlocking ResponseEntity.notFound().build()
                }

                val file = resource.file
                val contentType = Files.probeContentType(file.toPath())
                    ?: "application/octet-stream"

                logger.info("[$requestId] Logo image found for blog: $blogId, contentType: $contentType")

                ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${file.name}\"")
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600") // Cache 1 heure
                    .header("X-Request-Id", requestId)
                    .header("ngrok-skip-browser-warning", "true") // Pour ngrok
                    .body(resource)
            } catch (e: Exception) {
                logger.error("[$requestId] Error serving logo image for blog $blogId", e)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Récupérer l'image de couverture par nom de fichier
     * GET /api/blogs/cover-images/{filename}
     */
    @GetMapping("/cover-images/{filename:.+}")
    suspend fun getCoverImageByFilename(
        @PathVariable filename: String
    ): ResponseEntity<Resource> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get cover image by filename: $filename")

        return runBlocking {
            try {
                val resource = blogImageService.getCoverImageResourceByFilename(filename)
                if (resource == null || !resource.exists()) {
                    logger.warn("[$requestId] Cover image not found: $filename")
                    return@runBlocking ResponseEntity.notFound().build()
                }

                val file = resource.file
                val contentType = Files.probeContentType(file.toPath())
                    ?: "application/octet-stream"

                ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"$filename\"")
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                    .header("X-Request-Id", requestId)
                    .header("ngrok-skip-browser-warning", "true")
                    .body(resource)
            } catch (e: Exception) {
                logger.error("[$requestId] Error serving cover image: $filename", e)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    /**
     * Récupérer l'image logo par nom de fichier
     * GET /api/blogs/logo-images/{filename}
     */
    @GetMapping("/logo-images/{filename:.+}")
    suspend fun getLogoImageByFilename(
        @PathVariable filename: String
    ): ResponseEntity<Resource> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get logo image by filename: $filename")

        return runBlocking {
            try {
                val resource = blogImageService.getLogoImageResourceByFilename(filename)
                if (resource == null || !resource.exists()) {
                    logger.warn("[$requestId] Logo image not found: $filename")
                    return@runBlocking ResponseEntity.notFound().build()
                }

                val file = resource.file
                val contentType = Files.probeContentType(file.toPath())
                    ?: "application/octet-stream"

                ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"$filename\"")
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                    .header("X-Request-Id", requestId)
                    .header("ngrok-skip-browser-warning", "true")
                    .body(resource)
            } catch (e: Exception) {
                logger.error("[$requestId] Error serving logo image: $filename", e)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    @PostMapping("/{blogId}/cover-image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun uploadBlogCoverImage(
        @AuthenticationPrincipal userId: String,
        @PathVariable blogId: String,
        @RequestPart("file") filePart: FilePart
    ): ResponseEntity<ApiResponseDto<Map<String, String>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Upload cover image for blog: $blogId by user: $userId")

        val blog = blogService.getBlogById(ObjectId(blogId))
        if (blog.authorId != userId) {
            error("You are not authorized to modify this blog")
        }

        if (!blog.coverImageUrl.isNullOrBlank() && fileStorageService.isLocalFile(blog.coverImageUrl)) {
            fileStorageService.deleteBlogCoverImage(blog.coverImageUrl, ObjectId(userId))
        }

        val imageUrl = fileStorageService.storeBlogCoverImage(filePart, blogId, ObjectId(userId))
        blogService.updateBlog(ObjectId(blogId), UpdateBlogRequest(coverImageUrl = imageUrl), ObjectId(userId))

        return ResponseEntity.ok(
            ApiResponseDto.Companion.success(
                data = mapOf("coverImageUrl" to imageUrl, "blogId" to blogId),
                message = "Blog cover image uploaded successfully",
                requestId = requestId
            )
        )
    }

    @PostMapping("/{blogId}/logo-image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun uploadBlogLogoImage(
        @AuthenticationPrincipal userId: String,
        @PathVariable blogId: String,
        @RequestPart("file") filePart: FilePart
    ): ResponseEntity<ApiResponseDto<Map<String, String>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Upload logo image for blog: $blogId by user: $userId")

        val blog = blogService.getBlogById(ObjectId(blogId))
        if (blog.authorId != userId) {
            error("You are not authorized to modify this blog")
        }

        if (!blog.logoImageUrl.isNullOrBlank() && fileStorageService.isLocalFile(blog.logoImageUrl)) {
            fileStorageService.deleteBlogLogoImage(blog.logoImageUrl, ObjectId(userId))
        }

        val imageUrl = fileStorageService.storeBlogLogoImage(filePart, blogId, ObjectId(userId))
        blogService.updateBlog(ObjectId(blogId), UpdateBlogRequest(logoImageUrl = imageUrl), ObjectId(userId))

        return ResponseEntity.ok(
            ApiResponseDto.Companion.success(
                data = mapOf("logoImageUrl" to imageUrl, "blogId" to blogId),
                message = "Blog logo image uploaded successfully",
                requestId = requestId
            )
        )
    }

    @DeleteMapping("/{blogId}/cover-image")
    suspend fun deleteBlogCoverImage(
        @AuthenticationPrincipal userId: String,
        @PathVariable blogId: String
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Delete cover image for blog: $blogId by user: $userId")

        val blog = blogService.getBlogById(ObjectId(blogId))
        if (blog.authorId != userId) {
            error("You are not authorized to modify this blog")
        }

        if (!blog.coverImageUrl.isNullOrBlank() && fileStorageService.isLocalFile(blog.coverImageUrl)) {
            fileStorageService.deleteBlogCoverImage(blog.coverImageUrl)
        }

        blogService.updateBlog(ObjectId(blogId), UpdateBlogRequest(coverImageUrl = null), ObjectId(userId))

        return ResponseEntity.ok(
            ApiResponseDto.Companion.success(
                data = null,
                message = "Blog cover image deleted successfully",
                requestId = requestId
            )
        ) as ResponseEntity<ApiResponseDto<Nothing>>
    }

    @DeleteMapping("/{blogId}/logo-image")
    suspend fun deleteBlogLogoImage(
        @AuthenticationPrincipal userId: String,
        @PathVariable blogId: String
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Delete logo image for blog: $blogId by user: $userId")

        val blog = blogService.getBlogById(ObjectId(blogId))
        if (blog.authorId != userId) {
            error("You are not authorized to modify this blog")
        }

        if (!blog.logoImageUrl.isNullOrBlank() && fileStorageService.isLocalFile(blog.logoImageUrl)) {
            fileStorageService.deleteBlogLogoImage(blog.logoImageUrl)
        }

        blogService.updateBlog(ObjectId(blogId), UpdateBlogRequest(logoImageUrl = null), ObjectId(userId))

        return ResponseEntity.ok(
            ApiResponseDto.Companion.success(
                data = null,
                message = "Blog logo image deleted successfully",
                requestId = requestId
            )
        ) as ResponseEntity<ApiResponseDto<Nothing>>
    }
}