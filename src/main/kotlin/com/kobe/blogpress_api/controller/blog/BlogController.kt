package com.kobe.blogpress_api.controller.blog

import com.kobe.blogpress_api.dto.article.ArticleSummaryDto
import com.kobe.blogpress_api.dto.blog.BatchCreateBlogsRequestDTO
import com.kobe.blogpress_api.dto.blog.BlogGlobalStatsResponse
import com.kobe.blogpress_api.dto.blog.BlogResponse
import com.kobe.blogpress_api.dto.blog.BlogStats
import com.kobe.blogpress_api.dto.blog.BlogSummaryDto
import com.kobe.blogpress_api.dto.blog.CreateBlogRequest
import com.kobe.blogpress_api.dto.blog.UpdateBlogRequest
import com.kobe.blogpress_api.dto.common.ApiResponseDto
import com.kobe.blogpress_api.services.blog.BlogService
import jakarta.validation.Valid
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/blogs")
class BlogController(
    private val blogService: BlogService,
    private val articleRepository: com.kobe.blogpress_api.repository.article.ArticleRepository,
    private val articleService: com.kobe.blogpress_api.services.article.ArticleService
) {

    private val logger = LoggerFactory.getLogger(BlogController::class.java)

    @PostMapping
    suspend fun createBlog(
        @AuthenticationPrincipal userId: String,
        @Valid @RequestBody request: CreateBlogRequest
    ): ResponseEntity<ApiResponseDto<BlogResponse>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Create blog request from user: $userId")
        val blog = blogService.createBlog(request, ObjectId(userId))
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponseDto.success(data = blog, message = "Blog created successfully", requestId = requestId))
    }

    @PutMapping("/{blogId}")
    suspend fun updateBlog(
        @AuthenticationPrincipal userId: String,
        @PathVariable blogId: String,
        @Valid @RequestBody request: UpdateBlogRequest
    ): ResponseEntity<ApiResponseDto<BlogResponse>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Update blog request: $blogId by user: $userId")
        val blog = blogService.updateBlog(ObjectId(blogId), request, ObjectId(userId))
        return ResponseEntity.ok(ApiResponseDto.success(data = blog, message = "Blog updated successfully", requestId = requestId))
    }

    @DeleteMapping("/{blogId}")
    suspend fun deleteBlog(
        @AuthenticationPrincipal userId: String,
        @PathVariable blogId: String
    ): ResponseEntity<ApiResponseDto<Map<String, Any>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Delete blog request: $blogId by user: $userId")
        
        try {
            // Compter les articles avant suppression pour informer l'utilisateur
            val articlesCount = articleRepository.findAllByBlogId(ObjectId(blogId)).asFlow().toList().size
            
            blogService.deleteBlog(ObjectId(blogId), ObjectId(userId))
            
            return ResponseEntity.ok(
                ApiResponseDto.success(
                    data = mapOf(
                        "blogId" to blogId,
                        "deletedArticlesCount" to articlesCount,
                        "message" to "Blog and all associated articles deleted successfully"
                    ),
                    message = "Blog and all associated articles deleted successfully",
                    requestId = requestId
                )
            ) as ResponseEntity<ApiResponseDto<Map<String, Any>>>
        } catch (e: IllegalArgumentException) {
            logger.warn("[$requestId] Unauthorized deletion attempt: $blogId by user: $userId")
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponseDto.error<Map<String, Any>>(
                    message = e.message ?: "You are not authorized to delete this blog",
                    requestId = requestId
                ))
        } catch (e: Exception) {
            logger.error("[$requestId] Error deleting blog: $blogId", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error<Map<String, Any>>(
                    message = "Error deleting blog: ${e.message}",
                    requestId = requestId
                ))
        }
    }
    
    // ⭐ NOUVEAU : Supprimer tous les articles d'un blog sans supprimer le blog
    @DeleteMapping("/{blogId}/articles")
    suspend fun deleteAllBlogArticles(
        @AuthenticationPrincipal userId: String,
        @PathVariable blogId: String
    ): ResponseEntity<ApiResponseDto<Map<String, Any>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Delete all articles request for blog: $blogId by user: $userId")
        
        try {
            // Compter les articles avant suppression
            val articlesCount = articleRepository.findAllByBlogId(ObjectId(blogId)).asFlow().toList().size
            
            blogService.deleteAllBlogArticles(ObjectId(blogId), ObjectId(userId))
            
            return ResponseEntity.ok(
                ApiResponseDto.success(
                    data = mapOf(
                        "blogId" to blogId,
                        "deletedArticlesCount" to articlesCount,
                        "message" to "All articles deleted successfully"
                    ),
                    message = "All articles deleted successfully",
                    requestId = requestId
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("[$requestId] Unauthorized deletion attempt: $blogId by user: $userId")
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponseDto.error<Map<String, Any>>(
                    message = e.message ?: "You are not authorized to delete articles from this blog",
                    requestId = requestId
                ))
        } catch (e: Exception) {
            logger.error("[$requestId] Error deleting articles for blog: $blogId", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error<Map<String, Any>>(
                    message = "Error deleting articles: ${e.message}",
                    requestId = requestId
                ))
        }
    }

    @GetMapping("/slug/{slug}")
    suspend fun getBlogBySlug(
        @PathVariable slug: String,
        @AuthenticationPrincipal userId: String? = null
    ): ResponseEntity<ApiResponseDto<BlogResponse>> {
        val requestId = UUID.randomUUID().toString()
        // Convertir les chaînes vides en null
        val normalizedUserId = userId?.takeIf { it.isNotBlank() }
        logger.info("[$requestId] Get blog by slug: $slug, userId: $normalizedUserId")
        try {
            val userObjectId = normalizedUserId?.let { ObjectId(it) }
            val blog = blogService.getBlogBySlug(slug, userObjectId)
            return ResponseEntity.ok(ApiResponseDto.success(data = blog, message = "Blog retrieved successfully", requestId = requestId))
        } catch (e: Exception) {
            logger.error("[$requestId] Error retrieving blog by slug: $slug", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error(
                    message = "Erreur lors de la récupération du blog: ${e.message}",
                    requestId = requestId
                ))
        }
    }

    @GetMapping("/share/{shareId}")
    suspend fun getBlogByShareId(
        @PathVariable shareId: String,
        @AuthenticationPrincipal userId: String?
    ): ResponseEntity<ApiResponseDto<BlogResponse>> {
        val requestId = UUID.randomUUID().toString()
        // Convertir les chaînes vides en null
        val normalizedUserId = userId?.takeIf { it.isNotBlank() }
        logger.info("[$requestId] Get blog by shareId: $shareId, userId: $normalizedUserId")
        try {
            val userObjectId = normalizedUserId?.let { ObjectId(it) }
            val blog = blogService.getBlogByShareId(shareId, userObjectId)
            return ResponseEntity.ok(ApiResponseDto.success(data = blog, message = "Blog retrieved successfully", requestId = requestId))
        } catch (e: Exception) {
            logger.error("[$requestId] Error retrieving blog by shareId: $shareId", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error(
                    message = "Erreur lors de la récupération du blog: ${e.message}",
                    requestId = requestId
                ))
        }
    }

    @GetMapping("/{identifier}")
    suspend fun getBlogByIdOrSlug(
        @PathVariable identifier: String,
        @AuthenticationPrincipal userId: String? = null
    ): ResponseEntity<ApiResponseDto<BlogResponse>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get blog by identifier: $identifier")
        
        try {
            // Convertir les chaînes vides en null
            val normalizedUserId = userId?.takeIf { it.isNotBlank() }
            val userObjectId = normalizedUserId?.let { ObjectId(it) }
            
            // Détecter si c'est un ObjectId, un shareId (UUID), ou un slug
            val blog = when {
                ObjectId.isValid(identifier) -> {
                    // C'est un ObjectId
                    blogService.getBlogById(ObjectId(identifier))
                }
                isValidUUID(identifier) -> {
                    // C'est probablement un shareId (UUID)
                    blogService.getBlogByShareId(identifier, userObjectId)
                }
                else -> {
                    // C'est probablement un slug
                    blogService.getBlogBySlug(identifier, userObjectId)
                }
            }
            
            return ResponseEntity.ok(ApiResponseDto.success(data = blog, message = "Blog retrieved successfully", requestId = requestId))
        } catch (e: Exception) {
            logger.error("[$requestId] Error retrieving blog by identifier: $identifier", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error(
                    message = "Erreur lors de la récupération du blog: ${e.message}",
                    requestId = requestId
                ))
        }
    }
    
    private fun isValidUUID(uuid: String): Boolean {
        return try {
            java.util.UUID.fromString(uuid)
            true
        } catch (e: Exception) {
            false
        }
    }

    @GetMapping("/user")
    suspend fun getUserBlogs(
        @AuthenticationPrincipal userId: String,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(required = false, defaultValue = "desc") order: String?
    ): ResponseEntity<ApiResponseDto<Map<String, Any>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get user blogs for user: $userId - search=$search, status=$status, sortBy=$sortBy, order=$order")
        val blogs = blogService.getUserBlogs(
            ObjectId(userId),
            search = search,
            status = status,
            sortBy = sortBy,
            order = order
        )
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = mapOf("blogs" to blogs, "total" to blogs.size),
                message = "User blogs retrieved successfully",
                requestId = requestId
            )
        )
    }
    
    @GetMapping("/user/stats")
    suspend fun getUserBlogsStats(
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<ApiResponseDto<Map<String, Any>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get user blogs stats for user: $userId")
        val stats = blogService.getUserBlogsStats(ObjectId(userId))
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = stats,
                message = "Blog statistics retrieved successfully",
                requestId = requestId
            )
        )
    }

    @GetMapping("/favorites")
    suspend fun getFavoriteBlogs(
        @AuthenticationPrincipal userId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponseDto<List<BlogSummaryDto>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get favorite blogs for user: $userId - page=$page, size=$size")
        
        val blogs = blogService.getFavoriteBlogs(ObjectId(userId), page, size)
        
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = blogs,
                message = "Favorite blogs retrieved successfully",
                requestId = requestId
            )
        )
    }
    
    @PostMapping("/{blogId}/publish")
    suspend fun publishBlog(
        @AuthenticationPrincipal userId: String,
        @PathVariable blogId: String
    ): ResponseEntity<ApiResponseDto<Map<String, Any>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Publish blog request: $blogId by user: $userId")
        val blog = blogService.publishBlog(ObjectId(blogId), ObjectId(userId))
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = mapOf(
                    "blogId" to blog.id,
                    "isPublished" to blog.isPublished,
                    "publishedAt" to (blog.publishAt?.toString())
                ),
                message = "Blog published successfully",
                requestId = requestId
            )
        ) as ResponseEntity<ApiResponseDto<Map<String, Any>>>
    }
    
    @PostMapping("/{blogId}/unpublish")
    suspend fun unpublishBlog(
        @AuthenticationPrincipal userId: String,
        @PathVariable blogId: String
    ): ResponseEntity<ApiResponseDto<Map<String, Any>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Unpublish blog request: $blogId by user: $userId")
        val blog = blogService.unpublishBlog(ObjectId(blogId), ObjectId(userId))
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = mapOf(
                    "blogId" to blog.id,
                    "isPublished" to blog.isPublished,
                    "publishedAt" to null
                ),
                message = "Blog unpublished successfully",
                requestId = requestId
            )
        ) as ResponseEntity<ApiResponseDto<Map<String, Any>>>
    }
    
    @PostMapping("/{blogId}/duplicate")
    suspend fun duplicateBlog(
        @AuthenticationPrincipal userId: String,
        @PathVariable blogId: String
    ): ResponseEntity<ApiResponseDto<BlogResponse>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Duplicate blog request: $blogId by user: $userId")
        val duplicatedBlog = blogService.duplicateBlog(ObjectId(blogId), ObjectId(userId))
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                ApiResponseDto.success(
                    data = duplicatedBlog,
                    message = "Blog duplicated successfully",
                    requestId = requestId
                )
            )
    }

    @GetMapping
    suspend fun getPublishedBlogs(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponseDto<List<BlogSummaryDto>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get published blogs - page: $page, size: $size")
        val blogs = blogService.getPublishedBlogs(page, size)
        return ResponseEntity.ok(ApiResponseDto.success(data = blogs, message = "Published blogs retrieved successfully", requestId = requestId))
    }

    @GetMapping("/{blogId}/stats")
    suspend fun getBlogStats(
        @PathVariable blogId: String
    ): ResponseEntity<ApiResponseDto<BlogStats>> {
        val blog = blogService.getBlogById(ObjectId(blogId))
        val stats = BlogStats(
            viewCount = blog.viewCount,
            likeCount = blog.likeCount,
            shareCount = blog.shareCount,
            favoriteCount = blog.favoriteCount
        )
        return ResponseEntity.ok(ApiResponseDto.success(data = stats, message = "Blog stats retrieved"))
    }

    @GetMapping("/stats")
    suspend fun getGlobalStats(): ResponseEntity<ApiResponseDto<BlogGlobalStatsResponse>> {
        val stats = blogService.getGlobalStats()
        return ResponseEntity.ok(ApiResponseDto.success(data = stats, message = "Global blog stats retrieved"))
    }

    @GetMapping("/{blogId}/posts")
    suspend fun getBlogPosts(
        @PathVariable blogId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponseDto<Map<String, Any>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get blog posts for blog: $blogId - page: $page, size: $size")

        val articles = articleService.getBlogArticles(ObjectId(blogId), page, size).toList()

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = mapOf(
                    "posts" to articles,
                    "total" to articles.size,
                    "page" to page,
                    "size" to size
                ),
                message = "Blog posts retrieved successfully",
                requestId = requestId
            )
        )
    }

    /**
     * Créer plusieurs blogs en batch (pour les tests).
     *
     * POST /api/blogs/batch-create
     */
    @PostMapping("/batch-create")
    suspend fun batchCreateBlogs(
        @AuthenticationPrincipal userId: String,
        @Valid @RequestBody request: BatchCreateBlogsRequestDTO
    ): ResponseEntity<ApiResponseDto<Map<String, Any>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] User $userId creating ${request.count} blogs in batch")

        val result = blogService.batchCreateBlogs(request, ObjectId(userId))

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                ApiResponseDto.success(
                    data = result,
                    message = "${request.count} blogs created successfully",
                    requestId = requestId
                )
            )
    }
}