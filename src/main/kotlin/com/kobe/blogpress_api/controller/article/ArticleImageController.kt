package com.kobe.blogpress_api.controller.article

import com.kobe.blogpress_api.dto.common.ApiResponseDto
import com.kobe.blogpress_api.services.article.ArticleService
import com.kobe.blogpress_api.services.fileStorage.FileStorageService
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/articles")
class ArticleImageController(
    private val articleService: ArticleService,
    private val fileStorageService: FileStorageService
) {

    private val logger = LoggerFactory.getLogger(ArticleImageController::class.java)

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