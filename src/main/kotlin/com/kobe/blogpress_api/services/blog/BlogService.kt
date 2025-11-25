package com.kobe.blogpress_api.services.blog

import com.kobe.blogpress_api.domain.model.blog.Blog
import com.kobe.blogpress_api.dto.blog.BlogGlobalStatsResponse
import com.kobe.blogpress_api.dto.blog.BlogResponse
import com.kobe.blogpress_api.dto.blog.BlogStats
import com.kobe.blogpress_api.dto.blog.BlogSummaryDto
import com.kobe.blogpress_api.dto.blog.CreateBlogRequest
import com.kobe.blogpress_api.dto.blog.UpdateBlogRequest
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
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
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class BlogService(
    private val blogRepository: BlogRepository,
    private val blogSlugService: BlogSlugService,
    private val mongoTemplate: ReactiveMongoTemplate,
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
            publishAt = request.publishAt,
            tags = request.tags
        )
        val saved = blogRepository.save(blog).awaitSingle()
        return toBlogResponse(saved)
    }

    suspend fun updateBlog(blogId: ObjectId, request: UpdateBlogRequest, authorId: ObjectId): BlogResponse {
        val existing = findById(blogId)
        if (existing.authorId != authorId) {
            error("You are not authorized to update this blog")
        }
        val newSlug = if (request.title != null && request.title != existing.title) {
            blogSlugService.generateUniqueSlug(request.title, blogId)
        } else existing.slug
        val updated = existing.copy(
            title = request.title ?: existing.title,
            description = request.description ?: existing.description,
            slug = newSlug,
            logoImageUrl = request.logoImageUrl ?: existing.logoImageUrl,
            coverImageUrl = request.coverImageUrl ?: existing.coverImageUrl,
            tags = request.tags ?: existing.tags,
            isPublished = request.isPublished ?: existing.isPublished,
            isPrivate = request.isPrivate ?: existing.isPrivate,
            publishAt = request.publishAt ?: existing.publishAt,
            updatedAt = Instant.now()
        )
        val saved = blogRepository.save(updated).awaitSingle()
        return toBlogResponse(saved)
    }


    suspend fun deleteBlog(blogId: ObjectId, authorId: ObjectId) {
        val blog = findById(blogId)
        if (blog.authorId != authorId) {
            error("You are not authorized to delete this blog")
        }
        blogRepository.delete(blog).awaitSingleOrNull()
    }

    suspend fun getBlogBySlug(slug: String, userId: ObjectId? = null): BlogResponse {
        val blog = blogRepository.findBySlug(slug).awaitSingleOrNull()
            ?: error("Blog not found with slug: $slug")
        if (blog.isPrivate && blog.authorId != userId) {
            error("This blog is private")
        }
        if (!blog.isPublished && blog.authorId != userId) {
            error("This blog is not published yet")
        }
        if (blog.publishAt != null && blog.publishAt.isAfter(Instant.now()) && blog.authorId != userId) {
            error("Content not yet published")
        }
        return toBlogResponse(blog)
    }

    suspend fun getBlogById(blogId: ObjectId): BlogResponse {
        val blog = findById(blogId)
        return toBlogResponse(blog)
    }

    suspend fun getUserBlogs(authorId: ObjectId): Flow<BlogSummaryDto> {
        return blogRepository.findByAuthorId(authorId).asFlow().map { toBlogSummaryDto(it) }
    }

    suspend fun getPublishedBlogs(page: Int, size: Int): Flow<BlogSummaryDto> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return blogRepository.findByIsPublishedAndIsPrivate(true, false, pageable).asFlow().map { toBlogSummaryDto(it) }
    }

    // Incréments atomiques directement dans le service
    suspend fun incrementViewCount(blogId: ObjectId) {
        incrementField(blogId, "viewCount", 1)
    }

    suspend fun incrementLikeCount(blogId: ObjectId) {
        incrementField(blogId, "likeCount", 1)
    }

    suspend fun decrementLikeCount(blogId: ObjectId) {
        incrementField(blogId, "likeCount", -1)
    }

    suspend fun incrementFavoriteCount(blogId: ObjectId) {
        incrementField(blogId, "favoriteCount", 1)
    }

    suspend fun decrementFavoriteCount(blogId: ObjectId) {
        incrementField(blogId, "favoriteCount", -1)
    }

    suspend fun incrementShareCount(blogId: ObjectId) {
        incrementField(blogId, "shareCount", 1)
    }

    private suspend fun incrementField(blogId: ObjectId, field: String, delta: Long) {
        val query = Query(Criteria.where("_id").`is`(blogId))
        val update = Update().inc(field, delta)
        mongoTemplate.updateFirst(query, update, Blog::class.java).awaitSingleOrNull()
    }

    // Stats agrégées
    suspend fun getGlobalStats(): BlogGlobalStatsResponse {
        val all = blogRepository.findAll().collectList().awaitSingle()
        val totalBlogs = all.size.toLong()
        val totalViews = all.sumOf { it.viewCount }
        val totalLikes = all.sumOf { it.likeCount }
        val totalShares = all.sumOf { it.shareCount }
        val totalFavorites = all.sumOf { it.favoriteCount }
        return BlogGlobalStatsResponse(
            totalBlogs = totalBlogs,
            totalViews = totalViews,
            totalLikes = totalLikes,
            totalShares = totalShares,
            totalFavorites = totalFavorites
        )
    }

    private suspend fun findById(blogId: ObjectId): Blog {
        return blogRepository.findById(blogId).awaitSingleOrNull() ?: error("Blog not found with id: ${blogId.toHexString()}")
    }

    private fun toBlogResponse(blog: Blog): BlogResponse {
        return BlogResponse(
            id = blog.id.toHexString(),
            title = blog.title,
            description = blog.description,
            slug = blog.slug,
            logoImageUrl = blog.logoImageUrl,
            coverImageUrl = blog.coverImageUrl,
            tags = blog.tags,
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
            tags = blog.tags,
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