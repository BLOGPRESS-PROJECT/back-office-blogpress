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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import reactor.core.publisher.Flux
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
        
        // Si publishAt est défini et dans le futur, le blog ne doit pas être publié immédiatement
        val now = Instant.now()
        val shouldBePublished = when {
            request.publishAt != null -> {
                // Si publishAt est dans le passé ou maintenant, publier immédiatement
                // Sinon, ne pas publier (sera publié automatiquement par la tâche planifiée)
                !request.publishAt.isAfter(now) && request.isPublished
            }
            else -> request.isPublished
        }
        
        val blog = Blog(
            title = request.title,
            description = request.description,
            slug = slug,
            logoImageUrl = request.logoImageUrl,
            coverImageUrl = request.coverImageUrl,
            authorId = authorId,
            isPublished = shouldBePublished,
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
        // Normaliser les URLs d'images : convertir les chaînes vides en null
        val normalizedLogoImageUrl = request.logoImageUrl?.takeIf { it.isNotBlank() }
        val normalizedCoverImageUrl = request.coverImageUrl?.takeIf { it.isNotBlank() }
        
        // Gérer la publication programmée
        val now = Instant.now()
        val finalPublishAt = request.publishAt ?: existing.publishAt
        val finalIsPublished = when {
            request.isPublished != null -> {
                // Si l'utilisateur définit explicitement isPublished
                if (finalPublishAt != null && finalPublishAt.isAfter(now)) {
                    // Si publishAt est dans le futur, ne pas publier maintenant
                    false
                } else {
                    request.isPublished
                }
            }
            finalPublishAt != null && finalPublishAt.isAfter(now) -> {
                // Si publishAt est dans le futur et isPublished n'est pas défini, ne pas publier
                false
            }
            finalPublishAt != null && !finalPublishAt.isAfter(now) -> {
                // Si publishAt est dans le passé ou maintenant, publier
                true
            }
            else -> existing.isPublished
        }
        
        val updated = existing.copy(
            title = request.title ?: existing.title,
            description = request.description ?: existing.description,
            slug = newSlug,
            // Si une valeur est fournie (même null), l'utiliser. Sinon, garder l'ancienne valeur
            logoImageUrl = if (request.logoImageUrl != null) normalizedLogoImageUrl else existing.logoImageUrl,
            coverImageUrl = if (request.coverImageUrl != null) normalizedCoverImageUrl else existing.coverImageUrl,
            tags = request.tags ?: existing.tags,
            isPublished = finalIsPublished,
            isPrivate = request.isPrivate ?: existing.isPrivate,
            publishAt = finalPublishAt,
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

    suspend fun getUserBlogs(
        authorId: ObjectId,
        search: String? = null,
        status: String? = null,
        sortBy: String? = null,
        order: String? = "desc"
    ): List<BlogSummaryDto> {
        // Récupérer tous les blogs de l'utilisateur
        val allBlogs = blogRepository.findByAuthorId(authorId).collectList().awaitSingle()
        
        // Convertir en DTOs
        var blogs = allBlogs.map { toBlogSummaryDto(it) }
        
        // Filtrage par statut
        if (status != null && status != "all") {
            blogs = blogs.filter { blog ->
                when (status) {
                    "published" -> blog.isPublished && !blog.isPrivate
                    "draft" -> !blog.isPublished
                    "private" -> blog.isPrivate
                    else -> true
                }
            }
        }
        
        // Recherche dans titre/description
        if (!search.isNullOrBlank()) {
            val searchLower = search.lowercase()
            blogs = blogs.filter { blog ->
                blog.title.lowercase().contains(searchLower) ||
                (blog.description?.lowercase()?.contains(searchLower) == true)
            }
        }
        
        // Tri
        val sortedBlogs = when (sortBy) {
            "title" -> blogs.sortedBy { it.title.lowercase() }
            "updatedAt" -> blogs.sortedBy { it.updatedAt }
            "viewCount" -> blogs.sortedBy { it.stats.viewCount }
            "likeCount" -> blogs.sortedBy { it.stats.likeCount }
            "createdAt", null -> blogs.sortedBy { it.createdAt }
            else -> blogs
        }
        
        return if (order == "asc") {
            sortedBlogs
        } else {
            sortedBlogs.reversed()
        }
    }
    
    suspend fun publishBlog(blogId: ObjectId, authorId: ObjectId): BlogResponse {
        val blog = findById(blogId)
        if (blog.authorId != authorId) {
            error("You are not authorized to publish this blog")
        }
        
        val now = Instant.now()
        val updated = blog.copy(
            isPublished = true,
            publishAt = if (blog.publishAt == null || blog.publishAt.isAfter(now)) now else blog.publishAt,
            updatedAt = now
        )
        val saved = blogRepository.save(updated).awaitSingle()
        return toBlogResponse(saved)
    }
    
    suspend fun unpublishBlog(blogId: ObjectId, authorId: ObjectId): BlogResponse {
        val blog = findById(blogId)
        if (blog.authorId != authorId) {
            error("You are not authorized to unpublish this blog")
        }
        
        val updated = blog.copy(
            isPublished = false,
            updatedAt = Instant.now()
        )
        val saved = blogRepository.save(updated).awaitSingle()
        return toBlogResponse(saved)
    }
    
    suspend fun duplicateBlog(blogId: ObjectId, authorId: ObjectId): BlogResponse {
        val original = findById(blogId)
        if (original.authorId != authorId) {
            error("You are not authorized to duplicate this blog")
        }
        
        val newTitle = "${original.title} (copy)"
        val newSlug = blogSlugService.generateUniqueSlug(newTitle)
        
        val duplicated = Blog(
            title = newTitle,
            description = original.description,
            slug = newSlug,
            logoImageUrl = original.logoImageUrl,
            coverImageUrl = original.coverImageUrl,
            tags = original.tags,
            authorId = authorId,
            isPublished = false, // Le blog dupliqué n'est pas publié par défaut
            isPrivate = original.isPrivate,
            publishAt = null, // Pas de date de publication programmée pour la copie
            // Statistiques remises à 0
            postCount = 0,
            viewCount = 0,
            likeCount = 0,
            shareCount = 0,
            favoriteCount = 0
        )
        
        val saved = blogRepository.save(duplicated).awaitSingle()
        return toBlogResponse(saved)
    }
    
    suspend fun getUserBlogsStats(authorId: ObjectId): Map<String, Any> {
        val blogs = blogRepository.findByAuthorId(authorId).collectList().awaitSingle()
        
        val totalBlogs = blogs.size.toLong()
        val publishedBlogs = blogs.count { it.isPublished && !it.isPrivate }.toLong()
        val draftBlogs = blogs.count { !it.isPublished }.toLong()
        val privateBlogs = blogs.count { it.isPrivate }.toLong()
        
        val totalViews = blogs.sumOf { it.viewCount }
        val totalLikes = blogs.sumOf { it.likeCount }
        val totalShares = blogs.sumOf { it.shareCount }
        val totalFavorites = blogs.sumOf { it.favoriteCount }
        val totalPosts = blogs.sumOf { it.postCount }
        
        val averageViewsPerBlog = if (totalBlogs > 0) totalViews / totalBlogs else 0L
        
        val mostViewedBlog = blogs.maxByOrNull { it.viewCount }?.let { blog ->
            mapOf(
                "id" to blog.id.toHexString(),
                "title" to blog.title,
                "viewCount" to blog.viewCount
            )
        }
        
        return mapOf(
            "totalBlogs" to totalBlogs,
            "publishedBlogs" to publishedBlogs,
            "draftBlogs" to draftBlogs,
            "privateBlogs" to privateBlogs,
            "totalViews" to totalViews,
            "totalLikes" to totalLikes,
            "totalShares" to totalShares,
            "totalFavorites" to totalFavorites,
            "totalPosts" to totalPosts,
            "averageViewsPerBlog" to averageViewsPerBlog,
            "mostViewedBlog" to (mostViewedBlog ?: emptyMap<String, Any>())
        )
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
        val blogId = blog.id.toHexString()
        return BlogSummaryDto(
            id = blogId,
            title = blog.title,
            description = blog.description,
            slug = blog.slug,
            // ⚠️ IMPORTANT : Retourner les URLs API au lieu des chemins directs
            logoImageUrl = normalizeImageUrl(blog.logoImageUrl)?.let { url ->
                if (url.startsWith("/uploads/")) {
                    // Convertir le chemin en URL API
                    "/api/blogs/$blogId/logo-image"
                } else {
                    url // Garder les URLs externes telles quelles
                }
            },
            coverImageUrl = normalizeImageUrl(blog.coverImageUrl)?.let { url ->
                if (url.startsWith("/uploads/")) {
                    // Convertir le chemin en URL API
                    "/api/blogs/$blogId/cover-image"
                } else {
                    url // Garder les URLs externes telles quelles
                }
            },
            tags = blog.tags.takeIf { it.isNotEmpty() },
            authorId = blog.authorId.toHexString(),
            isPublished = blog.isPublished,
            isPrivate = blog.isPrivate,
            publicUrl = "$baseUrl/blog/${blog.slug}",
            createdAt = blog.createdAt.toString(), // ISO 8601 format
            updatedAt = blog.updatedAt.toString(), // ISO 8601 format
            postCount = blog.postCount,
            stats = BlogStats(
                viewCount = blog.viewCount,
                likeCount = blog.likeCount,
                shareCount = blog.shareCount,
                favoriteCount = blog.favoriteCount
            )
        )
    }

    /**
     * Normalise les URLs d'images pour le frontend :
     * - Retourne null si l'URL est null ou vide
     * - Retourne l'URL telle quelle si c'est une URL externe (http:// ou https://)
     * - S'assure que les chemins locaux commencent par "/"
     */
    private fun normalizeImageUrl(url: String?): String? {
        if (url.isNullOrBlank()) {
            return null
        }
        
        // Si c'est déjà une URL externe, la retourner telle quelle
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url
        }
        
        // Si c'est un chemin local, s'assurer qu'il commence par "/"
        return if (url.startsWith("/")) {
            url
        } else {
            "/$url"
        }
    }
}