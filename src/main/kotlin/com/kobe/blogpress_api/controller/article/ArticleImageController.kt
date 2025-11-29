package com.kobe.blogpress_api.controller.article

import com.kobe.blogpress_api.dto.common.ApiResponseDto
import com.kobe.blogpress_api.services.article.ArticleService
import com.kobe.blogpress_api.services.fileStorage.ArticleImageService
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
import java.util.*

@RestController
@RequestMapping("/api/articles/images")
class ArticleImageController(
    private val articleService: ArticleService,
    private val fileStorageService: FileStorageService,
    private val articleImageService: ArticleImageService
) {

    private val logger = LoggerFactory.getLogger(ArticleImageController::class.java)
    
    // ⭐ NOUVEAU : Servir l'image de couverture d'un article
    @GetMapping("/{articleId}/cover-image")
    suspend fun getArticleCoverImage(
        @PathVariable articleId: String
    ): ResponseEntity<Resource> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get cover image for article: $articleId")

        return runBlocking {
            try {
                val resource = articleImageService.getCoverImageResource(articleId)
                if (resource == null || !resource.exists()) {
                    logger.warn("[$requestId] Cover image not found for article: $articleId")
                    return@runBlocking ResponseEntity.notFound().build()
                }

                val file = resource.file
                val contentType = Files.probeContentType(file.toPath())
                    ?: "application/octet-stream"

                logger.info("[$requestId] Cover image found for article: $articleId, contentType: $contentType")

                ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${file.name}\"")
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600") // Cache 1 heure
                    .header("X-Request-Id", requestId)
                    .header("ngrok-skip-browser-warning", "true") // Pour ngrok
                    .body(resource)
            } catch (e: Exception) {
                logger.error("[$requestId] Error serving cover image for article $articleId", e)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

    @PostMapping("/{articleId}/cover-image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun uploadArticleCoverImage(
        @AuthenticationPrincipal userId: String,
        @PathVariable articleId: String,
        @RequestPart("file") filePart: FilePart
    ): ResponseEntity<ApiResponseDto<Map<String, String>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Upload cover image for article: $articleId by user: $userId")

        // Vérifier que l'utilisateur est propriétaire de l'article
        val article = articleService.getArticleById(ObjectId(articleId))
        if (article.authorId != userId) {
            throw IllegalArgumentException("You are not authorized to modify this article")
        }

        // Supprimer l'ancienne image si elle existe et est locale
        if (article.coverImageUrl != null && fileStorageService.isLocalFile(article.coverImageUrl)) {
            fileStorageService.deleteArticleCoverImage(article.coverImageUrl)
        }

        // Uploader la nouvelle image
        val imageUrl = fileStorageService.storeArticleCoverImage(filePart, articleId)

        // Mettre à jour l'article
        val updatedArticle = articleService.updateArticle(
            ObjectId(articleId),
            com.kobe.blogpress_api.dto.article.UpdateArticleRequest(coverImageUrl = imageUrl),
            ObjectId(userId)
        )

        logger.info("[$requestId] Article cover image uploaded successfully: $imageUrl")
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = mapOf(
                    "coverImageUrl" to imageUrl,
                    "articleId" to articleId
                ),
                message = "Article cover image uploaded successfully",
                requestId = requestId
            )
        )
    }

    @DeleteMapping("/{articleId}/cover-image")
    suspend fun deleteArticleCoverImage(
        @AuthenticationPrincipal userId: String,
        @PathVariable articleId: String
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Delete cover image for article: $articleId by user: $userId")

        val article = articleService.getArticleById(ObjectId(articleId))
        if (article.authorId != userId) {
            throw IllegalArgumentException("You are not authorized to modify this article")
        }

        if (article.coverImageUrl != null && fileStorageService.isLocalFile(article.coverImageUrl)) {
            fileStorageService.deleteArticleCoverImage(article.coverImageUrl)
        }

        articleService.updateArticle(
            ObjectId(articleId),
            com.kobe.blogpress_api.dto.article.UpdateArticleRequest(coverImageUrl = ""),
            ObjectId(userId)
        )

        logger.info("[$requestId] Article cover image deleted successfully")
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = null,
                message = "Article cover image deleted successfully",
                requestId = requestId
            )
        ) as ResponseEntity<ApiResponseDto<Nothing>>
    }
}