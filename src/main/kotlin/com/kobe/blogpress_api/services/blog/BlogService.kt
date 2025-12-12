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
import com.kobe.blogpress_api.services.fileStorage.FileStorageService
import com.kobe.blogpress_api.exception.BlogNotFoundException
import com.kobe.blogpress_api.exception.BlogNotPublishedException
import com.kobe.blogpress_api.exception.BlogPrivateException
import com.kobe.blogpress_api.exception.ContentNotYetPublishedException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import reactor.core.publisher.Flux
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
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
    private val fileStorageService: FileStorageService,
    private val articleRepository: com.kobe.blogpress_api.repository.article.ArticleRepository,
    private val favoriteRepository: com.kobe.blogpress_api.repository.interaction.FavoriteRepository,
    @Value("\${app.base-url:http://localhost:8090}") private val baseUrl: String,
    @Value("\${app.frontend-url:http://localhost:3000}") private val frontendUrl: String
) {

    private val logger = LoggerFactory.getLogger(BlogService::class.java)

    suspend fun createBlog(request: CreateBlogRequest, authorId: ObjectId): BlogResponse {
        val slug = blogSlugService.generateUniqueSlug(request.title)
        val shareId = java.util.UUID.randomUUID().toString() // Identifiant unique pour le partage
        
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
        
        // Générer l'URL publique (utiliser shareId pour garantir l'unicité)
        val publicUrl = "$frontendUrl/blog/$shareId"
        
        val blog = Blog(
            title = request.title,
            description = request.description,
            slug = slug,
            shareId = shareId,
            logoImageUrl = request.logoImageUrl,
            coverImageUrl = request.coverImageUrl,
            authorId = authorId,
            isPublished = shouldBePublished,
            isPrivate = request.isPrivate,
            publishAt = request.publishAt,
            lastPublishedAt = if (shouldBePublished) now else null,
            publicUrl = publicUrl,
            canonicalUrl = publicUrl, // Par défaut, l'URL publique est aussi l'URL canonique
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
        
        // Recalculer l'URL publique avec le shareId (qui reste inchangé)
        // Le shareId ne change jamais, donc l'URL publique reste la même
        val newPublicUrl = existing.publicUrl // Garder l'URL avec le shareId original
        
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
        
        // Mettre à jour lastPublishedAt si le blog vient d'être publié
        val newLastPublishedAt = when {
            !existing.isPublished && finalIsPublished -> now // Vient d'être publié
            else -> existing.lastPublishedAt
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
            lastPublishedAt = newLastPublishedAt,
            publicUrl = newPublicUrl,
            canonicalUrl = newPublicUrl, // Mettre à jour l'URL canonique aussi
            updatedAt = Instant.now()
        )
        val saved = blogRepository.save(updated).awaitSingle()
        return toBlogResponse(saved)
    }


    suspend fun deleteBlog(blogId: ObjectId, authorId: ObjectId) {
        val blog = findById(blogId)
        
        // Vérifier que l'utilisateur est le propriétaire du blog
        if (blog.authorId != authorId) {
            throw IllegalArgumentException("You are not authorized to delete this blog")
        }
        
        logger.info("Starting deletion of blog: ${blogId.toHexString()} and all associated articles")
        
        // 1. Supprimer tous les articles associés au blog
        try {
            val articles = articleRepository.findAllByBlogId(blogId).collectList().awaitSingle()
            logger.info("Found ${articles.size} articles to delete for blog: ${blogId.toHexString()}")
            
            // Supprimer les images de couverture de tous les articles
            articles.forEach { article ->
                try {
                    if (!article.coverImageUrl.isNullOrBlank() && fileStorageService.isLocalFile(article.coverImageUrl)) {
                        fileStorageService.deleteArticleCoverImage(article.coverImageUrl)
                        logger.debug("Article cover image deleted: ${article.id.toHexString()}")
                    }
                } catch (e: Exception) {
                    logger.warn("Error deleting cover image for article ${article.id.toHexString()}: ${e.message}", e)
                    // Continuer même si la suppression d'une image échoue
                }
            }
            
            // Supprimer tous les articles de la base de données
            if (articles.isNotEmpty()) {
                articleRepository.deleteByBlogId(blogId).awaitSingleOrNull()
                logger.info("Deleted ${articles.size} articles for blog: ${blogId.toHexString()}")
            }
        } catch (e: Exception) {
            logger.error("Error deleting articles for blog ${blogId.toHexString()}: ${e.message}", e)
            throw RuntimeException("Failed to delete articles for blog: ${e.message}", e)
        }
        
        // 2. Supprimer les images associées au blog
        try {
            // Supprimer l'image de couverture si elle existe et est un fichier local
            if (!blog.coverImageUrl.isNullOrBlank() && fileStorageService.isLocalFile(blog.coverImageUrl)) {
                fileStorageService.deleteBlogCoverImage(blog.coverImageUrl)
                logger.info("Cover image deleted for blog: ${blogId.toHexString()}")
            }
            
            // Supprimer l'image logo si elle existe et est un fichier local
            if (!blog.logoImageUrl.isNullOrBlank() && fileStorageService.isLocalFile(blog.logoImageUrl)) {
                fileStorageService.deleteBlogLogoImage(blog.logoImageUrl)
                logger.info("Logo image deleted for blog: ${blogId.toHexString()}")
            }
        } catch (e: Exception) {
            logger.warn("Error deleting images for blog ${blogId.toHexString()}: ${e.message}", e)
            // Continuer la suppression du blog même si la suppression des images échoue
        }
        
        // 3. Supprimer le blog de la base de données
        try {
            blogRepository.delete(blog).awaitSingleOrNull()
            logger.info("Blog deleted successfully: ${blogId.toHexString()}")
        } catch (e: Exception) {
            logger.error("Error deleting blog ${blogId.toHexString()}: ${e.message}", e)
            throw RuntimeException("Failed to delete blog: ${e.message}", e)
        }
    }
    
    /**
     * Supprimer tous les articles d'un blog sans supprimer le blog lui-même
     * Utile pour réinitialiser un blog ou nettoyer les articles
     */
    suspend fun deleteAllBlogArticles(blogId: ObjectId, authorId: ObjectId) {
        val blog = findById(blogId)
        
        // Vérifier que l'utilisateur est le propriétaire du blog
        if (blog.authorId != authorId) {
            throw IllegalArgumentException("You are not authorized to delete articles from this blog")
        }
        
        logger.info("Starting deletion of all articles for blog: ${blogId.toHexString()}")
        
        try {
            val articles = articleRepository.findAllByBlogId(blogId).collectList().awaitSingle()
            logger.info("Found ${articles.size} articles to delete for blog: ${blogId.toHexString()}")
            
            // Supprimer les images de couverture de tous les articles
            articles.forEach { article ->
                try {
                    if (!article.coverImageUrl.isNullOrBlank() && fileStorageService.isLocalFile(article.coverImageUrl)) {
                        fileStorageService.deleteArticleCoverImage(article.coverImageUrl)
                        logger.debug("Article cover image deleted: ${article.id.toHexString()}")
                    }
                } catch (e: Exception) {
                    logger.warn("Error deleting cover image for article ${article.id.toHexString()}: ${e.message}", e)
                }
            }
            
            // Supprimer tous les articles de la base de données
            if (articles.isNotEmpty()) {
                articleRepository.deleteByBlogId(blogId).awaitSingleOrNull()
                logger.info("Deleted ${articles.size} articles for blog: ${blogId.toHexString()}")
            }
            
            // Réinitialiser le compteur d'articles du blog
            val updatedBlog = blog.copy(postCount = 0)
            blogRepository.save(updatedBlog).awaitSingle()
            logger.info("Blog post count reset to 0 for blog: ${blogId.toHexString()}")
            
        } catch (e: Exception) {
            logger.error("Error deleting articles for blog ${blogId.toHexString()}: ${e.message}", e)
            throw RuntimeException("Failed to delete articles: ${e.message}", e)
        }
    }

    suspend fun getBlogBySlug(slug: String, userId: ObjectId? = null): BlogResponse {
        val blog = blogRepository.findBySlug(slug).awaitSingleOrNull()
            ?: throw BlogNotFoundException("Le blog recherché n'est pas disponible")
        
        // Le créateur peut toujours voir son blog, même s'il n'est pas publié
        val isOwner = blog.authorId == userId
        
        // Vérifications pour les utilisateurs non-propriétaires
        if (!isOwner) {
            if (blog.isPrivate) {
                throw BlogPrivateException("Ce blog est privé")
            }
            if (!blog.isPublished) {
                throw BlogNotPublishedException("Le blog recherché n'est pas encore disponible")
            }
            if (blog.publishAt != null && blog.publishAt.isAfter(Instant.now())) {
                throw ContentNotYetPublishedException(
                    blog.publishAt,
                    "Blog"
                )
            }
        }
        
        return toBlogResponse(blog)
    }
    
    suspend fun getBlogByShareId(shareId: String, userId: ObjectId? = null): BlogResponse {
        val blog = blogRepository.findByShareId(shareId).awaitSingleOrNull()
            ?: throw BlogNotFoundException("Le blog recherché n'est pas disponible")
        
        // Le créateur peut toujours voir son blog, même s'il n'est pas publié
        val isOwner = blog.authorId == userId
        
        // Vérifications pour les utilisateurs non-propriétaires
        if (!isOwner) {
            if (blog.isPrivate) {
                throw BlogPrivateException("Ce blog est privé")
            }
            if (!blog.isPublished) {
                throw BlogNotPublishedException("Le blog recherché n'est pas encore disponible")
            }
            if (blog.publishAt != null && blog.publishAt.isAfter(Instant.now())) {
                throw ContentNotYetPublishedException(
                    blog.publishAt,
                    "Blog"
                )
            }
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
            lastPublishedAt = now, // Mettre à jour la date de dernière publication
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
        val newShareId = java.util.UUID.randomUUID().toString() // Nouveau shareId unique
        val newPublicUrl = "$frontendUrl/blog/$newShareId"
        
        val duplicated = Blog(
            title = newTitle,
            description = original.description,
            slug = newSlug,
            shareId = newShareId,
            logoImageUrl = original.logoImageUrl,
            coverImageUrl = original.coverImageUrl,
            tags = original.tags,
            authorId = authorId,
            isPublished = false, // Le blog dupliqué n'est pas publié par défaut
            isPrivate = original.isPrivate,
            publishAt = null, // Pas de date de publication programmée pour la copie
            lastPublishedAt = null, // Pas de date de publication pour la copie
            publicUrl = newPublicUrl,
            canonicalUrl = newPublicUrl,
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

    suspend fun getFavoriteBlogs(userId: ObjectId, page: Int, size: Int): Flow<BlogSummaryDto> {
        val contentType = com.kobe.blogpress_api.domain.interaction.ContentType.BLOG
        
        // Récupérer les IDs des blogs favoris
        val favoriteIds = favoriteRepository.findByUserIdAndContentType(userId, contentType)
            .map { it.contentId }
            .collectList()
            .awaitSingle()
        
        if (favoriteIds.isEmpty()) {
            return kotlinx.coroutines.flow.emptyFlow()
        }
        
        // Récupérer les blogs correspondants
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val query = Query(Criteria.where("_id").`in`(favoriteIds))
            .with(pageable)
        
        return mongoTemplate.find(query, Blog::class.java)
            .asFlow()
            .map { toBlogSummaryDto(it) }
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
        val blogId = blog.id.toHexString()
        
        // Construire l'URL complète de l'image de couverture
        val coverImageUrl: String? = if (blog.coverImageUrl != null && blog.coverImageUrl.isNotBlank()) {
            if (blog.coverImageUrl.startsWith("http://") || blog.coverImageUrl.startsWith("https://")) {
                // URL externe complète, l'utiliser telle quelle
                blog.coverImageUrl
            } else {
                // Chemin relatif ou nom de fichier, construire l'URL complète
                "$baseUrl/api/blogs/$blogId/cover-image"
            }
        } else {
            null
        }
        
        // Construire l'URL complète du logo
        val logoImageUrl: String? = if (blog.logoImageUrl != null && blog.logoImageUrl.isNotBlank()) {
            if (blog.logoImageUrl.startsWith("http://") || blog.logoImageUrl.startsWith("https://")) {
                // URL externe complète, l'utiliser telle quelle
                blog.logoImageUrl
            } else {
                // Chemin relatif ou nom de fichier, construire l'URL complète
                "$baseUrl/api/blogs/$blogId/logo-image"
            }
        } else {
            null
        }
        
        return BlogResponse(
            id = blogId,
            title = blog.title,
            description = blog.description,
            slug = blog.slug,
            shareId = blog.shareId,
            logoImageUrl = logoImageUrl,   // ⭐ URL complète
            coverImageUrl = coverImageUrl, // ⭐ URL complète
            tags = blog.tags,
            authorId = blog.authorId.toHexString(),
            isPublished = blog.isPublished,
            isPrivate = blog.isPrivate,
            publishAt = blog.publishAt,
            publicUrl = blog.publicUrl, // Utiliser la valeur stockée en base
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
        
        // Construire l'URL complète de l'image de couverture
        val coverImageUrl: String? = if (blog.coverImageUrl != null && blog.coverImageUrl.isNotBlank()) {
            if (blog.coverImageUrl.startsWith("http://") || blog.coverImageUrl.startsWith("https://")) {
                // URL externe complète, l'utiliser telle quelle
                blog.coverImageUrl
            } else {
                // Chemin relatif ou nom de fichier, construire l'URL complète
                "$baseUrl/api/blogs/$blogId/cover-image"
            }
        } else {
            null
        }
        
        // Construire l'URL complète du logo
        val logoImageUrl: String? = if (blog.logoImageUrl != null && blog.logoImageUrl.isNotBlank()) {
            if (blog.logoImageUrl.startsWith("http://") || blog.logoImageUrl.startsWith("https://")) {
                // URL externe complète, l'utiliser telle quelle
                blog.logoImageUrl
            } else {
                // Chemin relatif ou nom de fichier, construire l'URL complète
                "$baseUrl/api/blogs/$blogId/logo-image"
            }
        } else {
            null
        }
        
        return BlogSummaryDto(
            id = blogId,
            title = blog.title,
            description = blog.description,
            slug = blog.slug,
            shareId = blog.shareId,
            logoImageUrl = logoImageUrl,   // ⭐ URL complète
            coverImageUrl = coverImageUrl, // ⭐ URL complète
            tags = blog.tags.takeIf { it.isNotEmpty() },
            authorId = blog.authorId.toHexString(),
            isPublished = blog.isPublished,
            isPrivate = blog.isPrivate,
            publicUrl = blog.publicUrl, // Utiliser la valeur stockée en base
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

}