package com.kobe.blogpress_api.controller.blog

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
    private val blogService: BlogService
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
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Delete blog request: $blogId by user: $userId")
        blogService.deleteBlog(ObjectId(blogId), ObjectId(userId))
        return ResponseEntity.ok(ApiResponseDto.success(data = null, message = "Blog deleted successfully", requestId = requestId)) as ResponseEntity<ApiResponseDto<Nothing>>
    }

    @GetMapping("/slug/{slug}")
    suspend fun getBlogBySlug(
        @PathVariable slug: String,
        @AuthenticationPrincipal userId: String?
    ): ResponseEntity<ApiResponseDto<BlogResponse>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get blog by slug: $slug")
        val userObjectId = userId?.let { ObjectId(it) }
        val blog = blogService.getBlogBySlug(slug, userObjectId)
        return ResponseEntity.ok(ApiResponseDto.success(data = blog, message = "Blog retrieved successfully", requestId = requestId))
    }

    @GetMapping("/{identifier}")
    suspend fun getBlogByIdOrSlug(
        @PathVariable identifier: String,
        @AuthenticationPrincipal userId: String?
    ): ResponseEntity<ApiResponseDto<BlogResponse>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get blog by identifier: $identifier")
        
        // Détecter si c'est un ObjectId ou un slug
        val blog = if (ObjectId.isValid(identifier)) {
            // C'est un ObjectId
            blogService.getBlogById(ObjectId(identifier))
        } else {
            // C'est probablement un slug
            val userObjectId = userId?.let { ObjectId(it) }
            blogService.getBlogBySlug(identifier, userObjectId)
        }
        
        return ResponseEntity.ok(ApiResponseDto.success(data = blog, message = "Blog retrieved successfully", requestId = requestId))
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
        val blogs = blogService.getPublishedBlogs(page, size).toList()
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
}