package com.kobe.blogpress_api.controller.feed

import com.kobe.blogpress_api.domain.model.article.ArticleType
import com.kobe.blogpress_api.dto.common.ApiResponseDto
import com.kobe.blogpress_api.dto.feed.FeedResponse
import com.kobe.blogpress_api.services.feed.FeedService
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/feed")
class FeedController(
    private val feedService: FeedService
) {

    private val logger = LoggerFactory.getLogger(FeedController::class.java)

    /**
     * GET /api/feed
     * Récupérer le feed principal (articles publics).
     * Authentification optionnelle (Bearer token).
     */
    @GetMapping
    suspend fun getFeed(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "createdAt,desc") sort: String,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) author: String?,
        @RequestParam(required = false) tags: String?,
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) search: String?,
        @AuthenticationPrincipal userId: String? // Optionnel
    ): ResponseEntity<ApiResponseDto<FeedResponse>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get feed request - page: $page, size: $size, sort: $sort, userId: $userId")

        try {
            // Valider les paramètres
            val validatedSize = size.coerceIn(1, 50)
            val validatedPage = page.coerceAtLeast(0)

            // Parser les tags
            val tagsList = tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }

            // Parser le type
            val articleType = when (type?.lowercase()) {
                "blog_post" -> ArticleType.BLOG_POST
                "simple_article" -> ArticleType.SIMPLE_ARTICLE
                "all", null -> null
                else -> {
                    logger.warn("[$requestId] Invalid type parameter: $type, using default: all")
                    null
                }
            }

            // Convertir userId en ObjectId si présent
            val userObjectId = userId?.takeIf { it.isNotBlank() }?.let {
                try {
                    ObjectId(it)
                } catch (e: IllegalArgumentException) {
                    logger.warn("[$requestId] Invalid userId format: $it")
                    null
                }
            }

            // Récupérer le feed
            val feedResponse = feedService.getFeed(
                page = validatedPage,
                size = validatedSize,
                sort = sort,
                category = category,
                author = author,
                tags = tagsList,
                type = articleType,
                search = search,
                userId = userObjectId
            )

            logger.info("[$requestId] Feed retrieved successfully - ${feedResponse.content.size} items")
            return ResponseEntity.ok(
                ApiResponseDto.success(
                    data = feedResponse,
                    message = "Feed retrieved successfully",
                    requestId = requestId
                )
            )
        } catch (e: Exception) {
            logger.error("[$requestId] Error retrieving feed", e)
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                    ApiResponseDto.error(
                        message = "Error retrieving feed: ${e.message}",
                        requestId = requestId
                    )
                )
        }
    }
}
