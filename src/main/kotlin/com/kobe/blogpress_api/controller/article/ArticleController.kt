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
@RequestMapping("/api")
class ArticleController(
    private val articleService: ArticleService
) {

    private val logger = LoggerFactory.getLogger(ArticleController::class.java)

    // ===== ARTICLES SIMPLES =====

    @PostMapping("/articles")
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

    @GetMapping("/articles/{slug}")
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

    @GetMapping("/articles")
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

    @GetMapping("/articles/user")
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

    @PutMapping("/articles/{articleId}")
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

    @DeleteMapping("/articles/{articleId}")
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