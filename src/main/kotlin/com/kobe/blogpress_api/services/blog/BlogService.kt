package com.kobe.blogpress_api.services.blog

import com.kobe.blogpress_api.domain.model.blog.Blog
import com.kobe.blogpress_api.dto.blog.BlogResponse
import com.kobe.blogpress_api.dto.blog.BlogStats
import com.kobe.blogpress_api.dto.blog.BlogSummaryDto
import com.kobe.blogpress_api.dto.blog.CreateBlogRequest
import com.kobe.blogpress_api.dto.blog.UpdateBlogRequest
import com.kobe.blogpress_api.exception.ResourceNotFoundException
import com.kobe.blogpress_api.repository.blog.BlogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class BlogService(
    private val blogRepository: BlogRepository,
    private val blogSlugService: BlogSlugService,
    @Value("\${app.base-url:http://localhost:8090}") private val baseUrl: String
) {

    suspend fun createBlog(request: CreateBlogRequest, authorId: ObjectId): BlogResponse {
        val slug = blogSlugService.generateUniqueSlug(request.title)

        val blog = Blog(
            title = request.title,
            description = request.description,
            slug = slug,
            logoImageUrl = request.logoImageUrl,
            coverImageUrl = request.coverImageUrl,
            authorId = authorId,
            isPublished = request.isPublished,
            isPrivate = request.isPrivate,
            publishAt = request.publishAt
        )

        val savedBlog = blogRepository.save(blog).awaitSingle()
        return toBlogResponse(savedBlog)
    }

    suspend fun updateBlog(blogId: ObjectId, request: UpdateBlogRequest, authorId: ObjectId): BlogResponse {
        val blog = findById(blogId)

        // Vérifier que l'utilisateur est l'auteur
        if (blog.authorId != authorId) {
            throw IllegalArgumentException("You are not authorized to update this blog")
        }

        // Générer nouveau slug si le titre change
        val newSlug = if (request.title != null && request.title != blog.title) {
            blogSlugService.generateUniqueSlug(request.title, blogId)
        } else {
            blog.slug
        }

        val updatedBlog = blog.copy(
            title = request.title ?: blog.title,
            description = request.description ?: blog.description,
            slug = newSlug,
            logoImageUrl = request.logoImageUrl ?: blog.logoImageUrl,
            coverImageUrl = request.coverImageUrl ?: blog.coverImageUrl,
            isPublished = request.isPublished ?: blog.isPublished,
            isPrivate = request.isPrivate ?: blog.isPrivate,
            publishAt = request.publishAt ?: blog.publishAt,
            updatedAt = Instant.now()
        )

        val savedBlog = blogRepository.save(updatedBlog).awaitSingle()
        return toBlogResponse(savedBlog)
    }

    suspend fun deleteBlog(blogId: ObjectId, authorId: ObjectId) {
        val blog = findById(blogId)

        // Vérifier que l'utilisateur est l'auteur
        if (blog.authorId != authorId) {
            throw IllegalArgumentException("You are not authorized to delete this blog")
        }

        blogRepository.delete(blog).awaitSingleOrNull()
    }

    suspend fun getBlogBySlug(slug: String): BlogResponse {
        val blog = blogRepository.findBySlug(slug).awaitSingleOrNull()
            ?: throw ResourceNotFoundException("Blog not found with slug: $slug")

        return toBlogResponse(blog)
    }

    suspend fun getBlogById(blogId: ObjectId): BlogResponse {
        val blog = findById(blogId)
        return toBlogResponse(blog)
    }

    suspend fun getUserBlogs(authorId: ObjectId): Flow<BlogSummaryDto> {
        return blogRepository.findByAuthorId(authorId)
            .asFlow()
            .map { toBlogSummaryDto(it) }
    }

    suspend fun getPublishedBlogs(page: Int, size: Int): Flow<BlogSummaryDto> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return blogRepository.findByIsPublishedAndIsPrivate(true, false, pageable)
            .asFlow()
            .map { toBlogSummaryDto(it) }
    }

    suspend fun incrementViewCount(blogId: ObjectId) {
        val blog = findById(blogId)
        val updatedBlog = blog.copy(viewCount = blog.viewCount + 1)
        blogRepository.save(updatedBlog).awaitSingle()
    }

    suspend fun incrementLikeCount(blogId: ObjectId) {
        val blog = findById(blogId)
        val updatedBlog = blog.copy(likeCount = blog.likeCount + 1)
        blogRepository.save(updatedBlog).awaitSingle()
    }

    suspend fun decrementLikeCount(blogId: ObjectId) {
        val blog = findById(blogId)
        val updatedBlog = blog.copy(likeCount = maxOf(0, blog.likeCount - 1))
        blogRepository.save(updatedBlog).awaitSingle()
    }

    suspend fun incrementFavoriteCount(blogId: ObjectId) {
        val blog = findById(blogId)
        val updatedBlog = blog.copy(favoriteCount = blog.favoriteCount + 1)
        blogRepository.save(updatedBlog).awaitSingle()
    }

    suspend fun decrementFavoriteCount(blogId: ObjectId) {
        val blog = findById(blogId)
        val updatedBlog = blog.copy(favoriteCount = maxOf(0, blog.favoriteCount - 1))
        blogRepository.save(updatedBlog).awaitSingle()
    }

    suspend fun incrementShareCount(blogId: ObjectId) {
        val blog = findById(blogId)
        val updatedBlog = blog.copy(shareCount = blog.shareCount + 1)
        blogRepository.save(updatedBlog).awaitSingle()
    }

    private suspend fun findById(blogId: ObjectId): Blog {
        return blogRepository.findById(blogId).awaitSingleOrNull()
            ?: throw ResourceNotFoundException("Blog not found with id: ${blogId.toHexString()}")
    }

    private fun toBlogResponse(blog: Blog): BlogResponse {
        return BlogResponse(
            id = blog.id.toHexString(),
            title = blog.title,
            description = blog.description,
            slug = blog.slug,
            logoImageUrl = blog.logoImageUrl,
            coverImageUrl = blog.coverImageUrl,
            authorId = blog.authorId.toHexString(),
            isPublished = blog.isPublished,
            isPrivate = blog.isPrivate,
            publishAt = blog.publishAt,
            publicUrl = "$baseUrl/blog/${blog.slug}",
            createdAt = blog.createdAt,
            updatedAt = blog.updatedAt,
            postCount = blog.postCount,
            viewCount = blog.viewCount,
            likeCount = blog.likeCount,
            shareCount = blog.shareCount,
            favoriteCount = blog.favoriteCount
        )
    }

    private fun toBlogSummaryDto(blog: Blog): BlogSummaryDto {
        return BlogSummaryDto(
            id = blog.id.toHexString(),
            title = blog.title,
            description = blog.description,
            slug = blog.slug,
            logoImageUrl = blog.logoImageUrl,
            coverImageUrl = blog.coverImageUrl,
            authorId = blog.authorId.toHexString(),
            isPublished = blog.isPublished,
            isPrivate = blog.isPrivate,
            publicUrl = "$baseUrl/blog/${blog.slug}",
            createdAt = blog.createdAt,
            updatedAt = blog.updatedAt,
            postCount = blog.postCount,
            stats = BlogStats(
                viewCount = blog.viewCount,
                likeCount = blog.likeCount,
                shareCount = blog.shareCount,
                favoriteCount = blog.favoriteCount
            )
        )
    }
}