package com.kobe.blogpress_api.controller.blog

import com.kobe.blogpress_api.dto.blog.BlogResponse
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

        logger.info("[$requestId] Blog created successfully: ${blog.id}")
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                ApiResponseDto.success(
                    data = blog,
                    message = "Blog created successfully",
                    requestId = requestId
                )
            )
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

        logger.info("[$requestId] Blog updated successfully: $blogId")
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = blog,
                message = "Blog updated successfully",
                requestId = requestId
            )
        )
    }

    @DeleteMapping("/{blogId}")
    suspend fun deleteBlog(
        @AuthenticationPrincipal userId: String,
        @PathVariable blogId: String
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Delete blog request: $blogId by user: $userId")

        blogService.deleteBlog(ObjectId(blogId), ObjectId(userId))

        logger.info("[$requestId] Blog deleted successfully: $blogId")
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = null,
                message = "Blog deleted successfully",
                requestId = requestId
            )
        ) as ResponseEntity<ApiResponseDto<Nothing>>
    }

    @GetMapping("/{slug}")
    suspend fun getBlogBySlug(
        @PathVariable slug: String,
        @AuthenticationPrincipal userId: String? // Optionnel pour les users non connectés
    ): ResponseEntity<ApiResponseDto<BlogResponse>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get blog by slug: $slug")

        val userObjectId = userId?.let { ObjectId(it) }
        val blog = blogService.getBlogBySlug(slug, userObjectId)

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = blog,
                message = "Blog retrieved successfully",
                requestId = requestId
            )
        )
    }

    @GetMapping("/user")
    suspend fun getUserBlogs(
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<ApiResponseDto<Map<String, Any>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get user blogs for user: $userId")

        val blogs = blogService.getUserBlogs(ObjectId(userId)).toList()

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = mapOf(
                    "blogs" to blogs,
                    "total" to blogs.size
                ),
                message = "User blogs retrieved successfully",
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

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = blogs,
                message = "Published blogs retrieved successfully",
                requestId = requestId
            )
        )
    }
}