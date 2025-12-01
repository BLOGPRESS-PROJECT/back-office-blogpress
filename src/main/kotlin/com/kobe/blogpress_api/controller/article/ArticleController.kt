package com.kobe.blogpress_api.controller.article

import com.kobe.blogpress_api.domain.model.article.ArticleType
import com.kobe.blogpress_api.dto.article.ArticleResponse
import com.kobe.blogpress_api.dto.article.ArticleSummaryDto
import com.kobe.blogpress_api.dto.article.CreateArticleRequest
import com.kobe.blogpress_api.dto.article.CreateBlogPostRequest
import com.kobe.blogpress_api.dto.article.UpdateArticleRequest
import com.kobe.blogpress_api.dto.common.ApiResponseDto
import com.kobe.blogpress_api.services.article.ArticleService
import jakarta.validation.Valid
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/articles")
class ArticleController(
    private val articleService: ArticleService
) {

    private val logger = LoggerFactory.getLogger(ArticleController::class.java)

    // ===== ARTICLES SIMPLES =====

    @PostMapping
    suspend fun createSimpleArticle(
        @AuthenticationPrincipal userId: String,
        @Valid @RequestBody request: CreateArticleRequest
    ): ResponseEntity<ApiResponseDto<ArticleResponse>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Create simple article request from user: $userId")

        val article = articleService.createSimpleArticle(request, ObjectId(userId))

        logger.info("[$requestId] Simple article created successfully: ${article.id}")
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                ApiResponseDto.success(
                    data = article,
                    message = "Simple article created successfully",
                    requestId = requestId
                )
            )
    }

    @GetMapping("/slug/{slug}")
    suspend fun getSimpleArticleBySlug(
        @PathVariable slug: String,
        @AuthenticationPrincipal userId: String?
    ): ResponseEntity<ApiResponseDto<ArticleResponse>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get simple article by slug: $slug")

        val userObjectId = userId?.let { ObjectId(it) }
        val article = articleService.getArticleBySlug(slug, userObjectId)

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = article,
                message = "Article retrieved successfully",
                requestId = requestId
            )
        )
    }
    
    // ⭐ NOUVEAU : Récupérer un article par son shareId
    @GetMapping("/share/{shareId}")
    suspend fun getArticleByShareId(
        @PathVariable shareId: String,
        @AuthenticationPrincipal userId: String?
    ): ResponseEntity<ApiResponseDto<ArticleResponse>> {
        val requestId = UUID.randomUUID().toString()
        val normalizedUserId = userId?.takeIf { it.isNotBlank() }
        logger.info("[$requestId] Get article by shareId: $shareId, userId: $normalizedUserId")
        
        try {
            val userObjectId = normalizedUserId?.let { ObjectId(it) }
            val article = articleService.getArticleByShareId(java.util.UUID.fromString(shareId), userObjectId)
            
            return ResponseEntity.ok(
                ApiResponseDto.success(
                    data = article,
                    message = "Article retrieved successfully",
                    requestId = requestId
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("[$requestId] Invalid shareId format: $shareId")
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDto.error(
                    message = "Format d'ID de partage invalide",
                    requestId = requestId
                ))
        } catch (e: com.kobe.blogpress_api.exception.ResourceNotFoundException) {
            logger.warn("[$requestId] Article not found with shareId: $shareId")
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.error(
                    message = "Article non trouvé",
                    requestId = requestId
                ))
        } catch (e: Exception) {
            logger.error("[$requestId] Error retrieving article by shareId: $shareId", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error(
                    message = "Erreur lors de la récupération de l'article",
                    requestId = requestId
                ))
        }
    }

    @GetMapping
    suspend fun getPublishedSimpleArticles(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponseDto<List<ArticleSummaryDto>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get published simple articles - page: $page, size: $size")

        val articles = articleService.getPublishedArticles(page, size, ArticleType.SIMPLE_ARTICLE).toList()

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = articles,
                message = "Published articles retrieved successfully",
                requestId = requestId
            )
        )
    }

    // ===== ARTICLES DE BLOG =====

    @PostMapping("/blogs/{blogId}/posts")
    suspend fun createBlogPost(
        @AuthenticationPrincipal userId: String,
        @PathVariable blogId: String,
        @Valid @RequestBody request: CreateBlogPostRequest
    ): ResponseEntity<ApiResponseDto<ArticleResponse>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Create blog post request from user: $userId for blog: $blogId")

        val article = articleService.createBlogPost(ObjectId(blogId), request, ObjectId(userId))

        logger.info("[$requestId] Blog post created successfully: ${article.id}")
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                ApiResponseDto.success(
                    data = article,
                    message = "Blog post created successfully",
                    requestId = requestId
                )
            )
    }

    @GetMapping("/blogs/{blogSlug}/posts/{postSlug}")
    suspend fun getBlogPostBySlug(
        @PathVariable blogSlug: String,
        @PathVariable postSlug: String,
        @AuthenticationPrincipal userId: String?
    ): ResponseEntity<ApiResponseDto<ArticleResponse>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get blog post: $blogSlug/$postSlug")

        val userObjectId = userId?.let { ObjectId(it) }
        val article = articleService.getBlogPostBySlug(blogSlug, postSlug, userObjectId)

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = article,
                message = "Blog post retrieved successfully",
                requestId = requestId
            )
        )
    }

    @GetMapping("/blogs/{blogId}/posts")
    suspend fun getBlogPosts(
        @PathVariable blogId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponseDto<List<ArticleSummaryDto>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get blog posts for blog: $blogId - page: $page, size: $size")

        val articles = articleService.getBlogArticles(ObjectId(blogId), page, size).toList()

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = articles,
                message = "Blog posts retrieved successfully",
                requestId = requestId
            )
        )
    }

    // ===== GESTION COMMUNE =====

    /**
     * ⚠️ Compatibilité ascendante :
     * GET /api/articles/{articleId} pour les anciens appels frontend qui utilisaient l'id MongoDB.
     * Pour les nouveaux flux publics, il est recommandé d'utiliser GET /api/articles/share/{shareId}.
     */
    @GetMapping("/{articleId}")
    suspend fun getArticleById(
        @PathVariable articleId: String,
        @AuthenticationPrincipal userId: String?
    ): ResponseEntity<ApiResponseDto<ArticleResponse>> {
        val requestId = UUID.randomUUID().toString()
        val normalizedUserId = userId?.takeIf { it.isNotBlank() }
        logger.info("[$requestId] Get article by id: $articleId, userId: $normalizedUserId")

        return try {
            val articleObjectId = ObjectId(articleId)
            val article = articleService.getArticleById(articleObjectId)

            ResponseEntity.ok(
                ApiResponseDto.success(
                    data = article,
                    message = "Article retrieved successfully",
                    requestId = requestId
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("[$requestId] Invalid articleId format: $articleId")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                    ApiResponseDto.error(
                        message = "Format d'identifiant d'article invalide",
                        requestId = requestId
                    )
                )
        } catch (e: com.kobe.blogpress_api.exception.ResourceNotFoundException) {
            logger.warn("[$requestId] Article not found with id: $articleId")
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                    ApiResponseDto.error(
                        message = "Article non trouvé",
                        requestId = requestId
                    )
                )
        } catch (e: Exception) {
            logger.error("[$requestId] Error retrieving article by id: $articleId", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                    ApiResponseDto.error(
                        message = "Erreur lors de la récupération de l'article",
                        requestId = requestId
                    )
                )
        }
    }

    @GetMapping("/user")
    suspend fun getUserArticles(
        @AuthenticationPrincipal userId: String,
        @RequestParam(required = false) type: ArticleType?
    ): ResponseEntity<ApiResponseDto<Map<String, Any>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get user articles for user: $userId, type: $type")

        val articles = articleService.getUserArticles(ObjectId(userId), type).toList()

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = mapOf(
                    "articles" to articles,
                    "total" to articles.size
                ),
                message = "User articles retrieved successfully",
                requestId = requestId
            )
        )
    }

    @GetMapping("/favorites")
    suspend fun getFavoriteArticles(
        @AuthenticationPrincipal userId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponseDto<List<ArticleSummaryDto>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get favorite articles for user: $userId - page=$page, size=$size")
        
        val articles = articleService.getFavoriteArticles(ObjectId(userId), page, size).toList()
        
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = articles,
                message = "Favorite articles retrieved successfully",
                requestId = requestId
            )
        )
    }

    @PutMapping("/{articleId}")
    suspend fun updateArticle(
        @AuthenticationPrincipal userId: String,
        @PathVariable articleId: String,
        @Valid @RequestBody request: UpdateArticleRequest
    ): ResponseEntity<ApiResponseDto<ArticleResponse>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Update article request: $articleId by user: $userId")

        val article = articleService.updateArticle(ObjectId(articleId), request, ObjectId(userId))

        logger.info("[$requestId] Article updated successfully: $articleId")
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = article,
                message = "Article updated successfully",
                requestId = requestId
            )
        )
    }

    @DeleteMapping("/{articleId}")
    suspend fun deleteArticle(
        @AuthenticationPrincipal userId: String,
        @PathVariable articleId: String
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Delete article request: $articleId by user: $userId")

        articleService.deleteArticle(ObjectId(articleId), ObjectId(userId))

        logger.info("[$requestId] Article deleted successfully: $articleId")
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = null,
                message = "Article deleted successfully",
                requestId = requestId
            )
        ) as ResponseEntity<ApiResponseDto<Nothing>>
    }
}