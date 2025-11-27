package com.kobe.blogpress_api.controller.images

import com.kobe.blogpress_api.dto.blog.UpdateBlogRequest
import com.kobe.blogpress_api.dto.common.ApiResponseDto
import com.kobe.blogpress_api.services.blog.BlogService
import com.kobe.blogpress_api.services.fileStorage.FileStorageService
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/blogs")
class BlogImageController(
    private val blogService: BlogService,
    private val fileStorageService: FileStorageService
) {

    private val logger = LoggerFactory.getLogger(BlogImageController::class.java)

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
            fileStorageService.deleteBlogCoverImage(blog.coverImageUrl)
        }

        val imageUrl = fileStorageService.storeBlogCoverImage(filePart, blogId)
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
            fileStorageService.deleteBlogLogoImage(blog.logoImageUrl)
        }

        val imageUrl = fileStorageService.storeBlogLogoImage(filePart, blogId)
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

        blogService.updateBlog(ObjectId(blogId), UpdateBlogRequest(coverImageUrl = ""), ObjectId(userId))

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

        blogService.updateBlog(ObjectId(blogId), UpdateBlogRequest(logoImageUrl = ""), ObjectId(userId))

        return ResponseEntity.ok(
            ApiResponseDto.Companion.success(
                data = null,
                message = "Blog logo image deleted successfully",
                requestId = requestId
            )
        ) as ResponseEntity<ApiResponseDto<Nothing>>
    }
}