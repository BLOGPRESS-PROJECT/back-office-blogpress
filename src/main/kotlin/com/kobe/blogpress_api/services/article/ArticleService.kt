package com.kobe.blogpress_api.services.article

import com.kobe.blogpress_api.domain.model.article.Article
import com.kobe.blogpress_api.domain.model.article.ArticleType
import com.kobe.blogpress_api.dto.article.ArticleResponse
import com.kobe.blogpress_api.dto.article.ArticleStats
import com.kobe.blogpress_api.dto.article.ArticleSummaryDto
import com.kobe.blogpress_api.dto.article.BatchCreateArticlesRequestDTO
import com.kobe.blogpress_api.dto.article.CreateArticleRequest
import com.kobe.blogpress_api.dto.article.CreateBlogPostRequest
import com.kobe.blogpress_api.dto.article.UpdateArticleRequest
import com.kobe.blogpress_api.exception.ContentNotYetPublishedException
import com.kobe.blogpress_api.exception.ResourceNotFoundException
import com.kobe.blogpress_api.repository.article.ArticleRepository
import com.kobe.blogpress_api.repository.blog.BlogRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Random
import java.util.UUID

@Service
class ArticleService(
    private val articleRepository: ArticleRepository,
    private val blogRepository: BlogRepository,
    private val articleSlugService: ArticleSlugService,
    private val favoriteRepository: com.kobe.blogpress_api.repository.interaction.FavoriteRepository,
    private val likeRepository: com.kobe.blogpress_api.repository.interaction.LikeRepository,
    private val mongoTemplate: org.springframework.data.mongodb.core.ReactiveMongoTemplate,
    private val fileStorageService: com.kobe.blogpress_api.services.fileStorage.FileStorageService,
    @Value("\${app.base-url:http://localhost:8090}") private val baseUrl: String,
    @Value("\${app.frontend-url:http://localhost:3000}") private val frontendUrl: String
) {

    private val logger = LoggerFactory.getLogger(ArticleService::class.java)

    // ===== CRÉATION =====

    suspend fun createSimpleArticle(request: CreateArticleRequest, authorId: ObjectId): ArticleResponse {
        val slug = articleSlugService.generateUniqueSlug(request.title)
        val readTime = calculateReadTime(request.content)

        // Si publishAt est défini et dans le futur, l'article ne doit pas être publié immédiatement
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
        val shareId = UUID.randomUUID()
        val publicUrl = "$frontendUrl/article/$shareId"

        val article = Article(
            title = request.title,
            content = request.content,
            excerpt = request.excerpt,
            slug = slug,
            coverImageUrl = request.coverImageUrl,
            tags = request.tags ?: emptyList(),
            category = request.category,
            authorId = authorId,
            blogId = null,
            type = ArticleType.SIMPLE_ARTICLE,
            isPublished = shouldBePublished,
            isPrivate = request.isPrivate,
            publishAt = request.publishAt,
            shareId = shareId,
            publicUrl = publicUrl,
            canonicalUrl = publicUrl, // Par défaut, l'URL publique est aussi l'URL canonique
            readTime = readTime
        )

        val savedArticle = articleRepository.save(article).awaitSingle()
        return toArticleResponse(savedArticle)
    }

    suspend fun createBlogPost(
        blogId: ObjectId,
        request: CreateBlogPostRequest,
        authorId: ObjectId
    ): ArticleResponse {
        // Vérifier que le blog existe et appartient à l'utilisateur
        val blog = blogRepository.findById(blogId).awaitSingleOrNull()
            ?: throw ResourceNotFoundException("Blog not found with id: ${blogId.toHexString()}")

        if (blog.authorId != authorId) {
            throw IllegalArgumentException("You are not authorized to create posts in this blog")
        }

        val slug = articleSlugService.generateUniqueSlug(request.title, blogId = blogId)
        val readTime = calculateReadTime(request.content)

        // Si publishAt est défini et dans le futur, l'article ne doit pas être publié immédiatement
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
        val shareId = UUID.randomUUID()
        val blogShareId = blog.shareId
        val publicUrl = "$frontendUrl/blog/$blogShareId/post/$shareId"

        val article = Article(
            title = request.title,
            content = request.content,
            excerpt = request.excerpt,
            slug = slug,
            coverImageUrl = request.coverImageUrl,
            tags = request.tags ?: emptyList(),
            category = request.category,
            authorId = authorId,
            blogId = blogId,
            type = ArticleType.BLOG_POST,
            isPublished = shouldBePublished,
            isPrivate = request.isPrivate,
            publishAt = request.publishAt,
            shareId = shareId,
            publicUrl = publicUrl,
            canonicalUrl = publicUrl, // Par défaut, l'URL publique est aussi l'URL canonique
            readTime = readTime
        )

        val savedArticle = articleRepository.save(article).awaitSingle()

        // Incrémenter le compteur d'articles du blog
        val updatedBlog = blog.copy(postCount = blog.postCount + 1)
        blogRepository.save(updatedBlog).awaitSingle()

        return toArticleResponse(savedArticle)
    }

    // ===== LECTURE =====

    suspend fun getArticleBySlug(slug: String, userId: ObjectId? = null): ArticleResponse {
        val article = articleRepository.findBySlug(slug).awaitSingleOrNull()
            ?: throw ResourceNotFoundException("Article not found with slug: $slug")

        checkArticleAccess(article, userId)

        return toArticleResponse(article)
    }

    suspend fun getBlogPostBySlug(
        blogSlug: String,
        postSlug: String,
        userId: ObjectId? = null
    ): ArticleResponse {
        val blog = blogRepository.findBySlug(blogSlug).awaitSingleOrNull()
            ?: throw ResourceNotFoundException("Blog not found with slug: $blogSlug")

        val article = articleRepository.findByBlogIdAndSlug(blog.id, postSlug).awaitSingleOrNull()
            ?: throw ResourceNotFoundException("Article not found with slug: $postSlug in blog: $blogSlug")

        checkArticleAccess(article, userId)

        return toArticleResponse(article)
    }

    suspend fun getArticleById(articleId: ObjectId): ArticleResponse {
        val article = findById(articleId)
        return toArticleResponse(article)
    }
    
    // ⭐ NOUVEAU : Récupérer un article par son shareId
    suspend fun getArticleByShareId(shareId: java.util.UUID, userId: ObjectId? = null): ArticleResponse {
        val article = articleRepository.findByShareId(shareId).awaitSingleOrNull()
            ?: throw ResourceNotFoundException("Article not found with shareId: $shareId")
        
        // Le créateur peut toujours voir son article, même s'il n'est pas publié
        val isOwner = article.authorId == userId
        
        // Vérifications pour les utilisateurs non-propriétaires
        if (!isOwner) {
            checkArticleAccess(article, userId)
        }
        
        return toArticleResponse(article)
    }

    suspend fun getUserArticles(authorId: ObjectId, type: ArticleType? = null): Flow<ArticleSummaryDto> {
        return if (type != null) {
            val pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt"))
            articleRepository.findByAuthorIdAndType(authorId, type, pageable)
        } else {
            articleRepository.findByAuthorId(authorId)
        }
            .asFlow()
            .map { toArticleSummaryDto(it) }
    }

    suspend fun getBlogArticles(blogId: ObjectId, page: Int, size: Int): Flow<ArticleSummaryDto> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return articleRepository.findByBlogIdAndIsPublishedAndIsPrivate(blogId, true, false, pageable)
            .asFlow()
            .map { toArticleSummaryDto(it) }
    }

    suspend fun getPublishedArticles(page: Int, size: Int, type: ArticleType? = null): Flow<ArticleSummaryDto> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return if (type != null) {
            articleRepository.findByIsPublishedAndIsPrivateAndType(true, false, type, pageable)
        } else {
            articleRepository.findByIsPublishedAndIsPrivate(true, false, pageable)
        }
            .asFlow()
            .map { toArticleSummaryDto(it) }
    }

    suspend fun getFavoriteArticles(userId: ObjectId, page: Int, size: Int): Flow<ArticleSummaryDto> {
        val contentType = com.kobe.blogpress_api.domain.interaction.ContentType.ARTICLE
        
        // Récupérer les IDs des articles favoris
        val favoriteIds = favoriteRepository.findByUserIdAndContentType(userId, contentType)
            .map { it.contentId }
            .collectList()
            .awaitSingle()
        
        if (favoriteIds.isEmpty()) {
            return kotlinx.coroutines.flow.emptyFlow()
        }
        
        // Récupérer les articles correspondants
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val query = Query(Criteria.where("_id").`in`(favoriteIds)).with(pageable)
        
        return mongoTemplate.find(query, Article::class.java)
            .asFlow()
            .map { toArticleSummaryDto(it) }
    }

    // ===== MISE À JOUR =====

    suspend fun updateArticle(articleId: ObjectId, request: UpdateArticleRequest, authorId: ObjectId): ArticleResponse {
        val article = findById(articleId)

        // Vérifier que l'utilisateur est l'auteur
        if (article.authorId != authorId) {
            throw IllegalArgumentException("You are not authorized to update this article")
        }

        // Générer nouveau slug si le titre change
        val newSlug = if (request.title != null && request.title != article.title) {
            articleSlugService.generateUniqueSlug(request.title, articleId, article.blogId)
        } else {
            article.slug
        }

        // Recalculer le temps de lecture si le contenu change
        val newReadTime = if (request.content != null && request.content != article.content) {
            calculateReadTime(request.content)
        } else {
            article.readTime
        }

        // Gérer la publication programmée
        val now = Instant.now()
        val finalPublishAt = request.publishAt ?: article.publishAt
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
            else -> article.isPublished
        }

        // Recalculer l'URL publique avec le shareId (qui reste inchangé)
        // Le shareId ne change jamais, donc l'URL publique reste la même
        val newPublicUrl = article.publicUrl // Garder l'URL avec le shareId original

        // Gérer la suppression de l'ancienne image de couverture si une nouvelle est fournie
        val finalCoverImageUrl = if (request.coverImageUrl != null) {
            // Si une nouvelle image est fournie, supprimer l'ancienne si elle existe et est locale
            if (article.coverImageUrl != null && 
                article.coverImageUrl.isNotBlank() && 
                article.coverImageUrl != request.coverImageUrl &&
                fileStorageService.isLocalFile(article.coverImageUrl)) {
                try {
                    fileStorageService.deleteArticleCoverImage(article.coverImageUrl, article.authorId)
                    logger.debug("Old cover image deleted for article: ${article.id.toHexString()}")
                } catch (e: Exception) {
                    logger.warn("Error deleting old cover image for article ${article.id.toHexString()}: ${e.message}", e)
                    // Continuer même si la suppression échoue
                }
            }
            // Normaliser : convertir les chaînes vides en null
            request.coverImageUrl.takeIf { it.isNotBlank() }
        } else {
            // Aucune nouvelle image fournie, garder l'ancienne
            article.coverImageUrl
        }

        val updatedArticle = article.copy(
            title = request.title ?: article.title,
            content = request.content ?: article.content,
            excerpt = request.excerpt ?: article.excerpt,
            slug = newSlug,
            coverImageUrl = finalCoverImageUrl,
            tags = request.tags ?: article.tags,
            category = request.category ?: article.category,
            isPublished = finalIsPublished,
            isPrivate = request.isPrivate ?: article.isPrivate,
            publishAt = finalPublishAt,
            publicUrl = newPublicUrl, // Garder l'URL publique (shareId ne change jamais)
            canonicalUrl = newPublicUrl, // Mettre à jour l'URL canonique aussi
            readTime = newReadTime,
            updatedAt = Instant.now()
        )

        val savedArticle = articleRepository.save(updatedArticle).awaitSingle()

        return toArticleResponse(savedArticle)
    }

    // ===== SUPPRESSION =====

    suspend fun deleteArticle(articleId: ObjectId, authorId: ObjectId) {
        val article = findById(articleId)

        // Vérifier que l'utilisateur est l'auteur
        if (article.authorId != authorId) {
            throw IllegalArgumentException("You are not authorized to delete this article")
        }

        // Supprimer l'image de couverture si elle existe et est locale
        try {
            if (!article.coverImageUrl.isNullOrBlank() && fileStorageService.isLocalFile(article.coverImageUrl)) {
                fileStorageService.deleteArticleCoverImage(article.coverImageUrl, article.authorId)
                logger.debug("Article cover image deleted: ${article.id.toHexString()}")
            }
        } catch (e: Exception) {
            logger.warn("Error deleting cover image for article ${article.id.toHexString()}: ${e.message}", e)
            // Continuer même si la suppression de l'image échoue
        }

        // Supprimer les interactions (likes et favorites) de l'article
        try {
            likeRepository.deleteByContentIdAndContentType(
                articleId,
                com.kobe.blogpress_api.domain.interaction.ContentType.ARTICLE
            ).awaitSingleOrNull()
            favoriteRepository.deleteByContentIdAndContentType(
                articleId,
                com.kobe.blogpress_api.domain.interaction.ContentType.ARTICLE
            ).awaitSingleOrNull()
            logger.debug("Interactions deleted for article: ${article.id.toHexString()}")
        } catch (e: Exception) {
            logger.warn("Error deleting interactions for article ${article.id.toHexString()}: ${e.message}", e)
            // Continuer même si la suppression des interactions échoue
        }

        // Décrémenter le compteur du blog si c'est un BLOG_POST
        if (article.blogId != null) {
            val blog = blogRepository.findById(article.blogId).awaitSingleOrNull()
            if (blog != null) {
                val updatedBlog = blog.copy(postCount = maxOf(0, blog.postCount - 1))
                blogRepository.save(updatedBlog).awaitSingle()
            }
        }

        articleRepository.delete(article).awaitSingleOrNull()
    }

    // ===== COMPTEURS =====

    suspend fun incrementViewCount(articleId: ObjectId) {
        val article = findById(articleId)
        val updatedArticle = article.copy(viewCount = article.viewCount + 1)
        articleRepository.save(updatedArticle).awaitSingle()
    }

    suspend fun incrementLikeCount(articleId: ObjectId) {
        val article = findById(articleId)
        val updatedArticle = article.copy(likeCount = article.likeCount + 1)
        articleRepository.save(updatedArticle).awaitSingle()
    }

    suspend fun decrementLikeCount(articleId: ObjectId) {
        val article = findById(articleId)
        val updatedArticle = article.copy(likeCount = maxOf(0, article.likeCount - 1))
        articleRepository.save(updatedArticle).awaitSingle()
    }

    suspend fun incrementShareCount(articleId: ObjectId) {
        val article = findById(articleId)
        val updatedArticle = article.copy(shareCount = article.shareCount + 1)
        articleRepository.save(updatedArticle).awaitSingle()
    }

    suspend fun incrementFavoriteCount(articleId: ObjectId) {
        val article = findById(articleId)
        val updatedArticle = article.copy(favoriteCount = article.favoriteCount + 1)
        articleRepository.save(updatedArticle).awaitSingle()
    }

    suspend fun decrementFavoriteCount(articleId: ObjectId) {
        val article = findById(articleId)
        val updatedArticle = article.copy(favoriteCount = maxOf(0, article.favoriteCount - 1))
        articleRepository.save(updatedArticle).awaitSingle()
    }

    // ===== HELPERS =====

    private suspend fun findById(articleId: ObjectId): Article {
        return articleRepository.findById(articleId).awaitSingleOrNull()
            ?: throw ResourceNotFoundException("Article not found with id: ${articleId.toHexString()}")
    }

    private suspend fun checkArticleAccess(article: Article, userId: ObjectId?) {
        // Vérifier si l'article est privé
        if (article.isPrivate && article.authorId != userId) {
            throw IllegalArgumentException("This article is private")
        }

        // Vérifier si l'article est publié
        if (!article.isPublished) {
            if (article.authorId != userId) {
                throw IllegalArgumentException("This article is not published yet")
            }
        }

        // Vérifier la date de publication programmée
        if (article.publishAt != null && article.publishAt.isAfter(Instant.now())) {
            if (article.authorId != userId) {
                throw ContentNotYetPublishedException(article.publishAt, "Article")
            }
        }
    }

    private fun calculateReadTime(content: String): Int {
        // Supprimer les balises HTML pour compter les mots
        val textContent = content.replace(Regex("<[^>]*>"), " ")

        // Compter les mots (séparés par des espaces)
        val wordCount = textContent.split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .size

        // Vitesse de lecture moyenne : 200 mots par minute
        val readTime = (wordCount / 200.0).toInt()

        // Minimum 1 minute, maximum 60 minutes
        return maxOf(1, minOf(60, readTime))
    }

    private suspend fun toArticleResponse(article: Article): ArticleResponse {
        // Construire l'URL complète de l'image de couverture
        val coverImageUrl: String? = if (article.coverImageUrl != null && article.coverImageUrl.isNotBlank()) {
            if (article.coverImageUrl.startsWith("http://") || article.coverImageUrl.startsWith("https://")) {
                // URL externe complète, l'utiliser telle quelle
                article.coverImageUrl
            } else {
                // Chemin relatif ou nom de fichier, construire l'URL complète
                "$baseUrl/api/articles/images/${article.id.toHexString()}/cover-image"
            }
        } else {
            null
        }

        return ArticleResponse(
            id = article.id.toHexString(),
            title = article.title,
            content = article.content,
            excerpt = article.excerpt,
            slug = article.slug,
            shareId = article.shareId.toString(),
            coverImageUrl = coverImageUrl,
            tags = article.tags,
            category = article.category,
            authorId = article.authorId.toHexString(),
            blogId = article.blogId?.toHexString(),
            type = article.type,
            isPublished = article.isPublished,
            isPrivate = article.isPrivate,
            publishAt = article.publishAt,
            publicUrl = article.publicUrl, // Utiliser l'URL stockée en base
            createdAt = article.createdAt,
            updatedAt = article.updatedAt,
            viewCount = article.viewCount,
            likeCount = article.likeCount,
            commentCount = article.commentCount,
            shareCount = article.shareCount,
            favoriteCount = article.favoriteCount,
            readTime = article.readTime
        )
    }

    private suspend fun toArticleSummaryDto(article: Article): ArticleSummaryDto {
        // Construire l'URL complète de l'image de couverture
        val coverImageUrl: String? = if (article.coverImageUrl != null && article.coverImageUrl.isNotBlank()) {
            if (article.coverImageUrl.startsWith("http://") || article.coverImageUrl.startsWith("https://")) {
                // URL externe complète, l'utiliser telle quelle
                article.coverImageUrl
            } else {
                // Chemin relatif ou nom de fichier, construire l'URL complète
                "$baseUrl/api/articles/images/${article.id.toHexString()}/cover-image"
            }
        } else {
            null
        }

        return ArticleSummaryDto(
            id = article.id.toHexString(),
            title = article.title,
            excerpt = article.excerpt,
            slug = article.slug,
            shareId = article.shareId.toString(),
            coverImageUrl = coverImageUrl,
            tags = article.tags,
            category = article.category,
            authorId = article.authorId.toHexString(),
            blogId = article.blogId?.toHexString(),
            type = article.type,
            isPublished = article.isPublished,
            isPrivate = article.isPrivate,
            publicUrl = article.publicUrl, // Utiliser l'URL stockée en base
            createdAt = article.createdAt,
            updatedAt = article.updatedAt,
            readTime = article.readTime,
            stats = ArticleStats(
                viewCount = article.viewCount,
                likeCount = article.likeCount,
                commentCount = article.commentCount,
                shareCount = article.shareCount,
                favoriteCount = article.favoriteCount
            )
        )
    }

    /**
     * Créer plusieurs articles simples en batch pour les tests.
     */
    suspend fun batchCreateArticles(request: BatchCreateArticlesRequestDTO, authorId: ObjectId): Map<String, Any> = coroutineScope {
        val random = Random()
        val categories = listOf("Technologie", "Voyage", "Cuisine", "Sport", "Culture", "Science", "Art", "Lifestyle", "Business", "Santé")
        val tagsList = listOf(
            listOf("Tech", "Innovation"),
            listOf("Voyage", "Aventure"),
            listOf("Cuisine", "Recettes"),
            listOf("Sport", "Fitness"),
            listOf("Culture", "Histoire"),
            listOf("Science", "Recherche"),
            listOf("Art", "Design"),
            listOf("Lifestyle", "Bien-être"),
            listOf("Business", "Entrepreneuriat"),
            listOf("Santé", "Médecine")
        )
        val contentTemplates = listOf(
            "<p>Cet article explore les dernières tendances dans le domaine de la technologie. Nous allons examiner comment les innovations récentes transforment notre quotidien.</p><p>Les développements technologiques continuent d'évoluer à un rythme rapide, offrant de nouvelles opportunités et défis.</p>",
            "<p>Découvrez les plus beaux endroits à visiter cette année. Ce guide vous emmène à travers des destinations incroyables qui valent vraiment le détour.</p><p>Chaque destination a son charme unique et offre des expériences mémorables pour les voyageurs.</p>",
            "<p>Apprenez à préparer des plats délicieux avec ces recettes faciles à suivre. La cuisine est un art qui se perfectionne avec la pratique.</p><p>Ces recettes sont parfaites pour les débutants comme pour les cuisiniers expérimentés.</p>",
            "<p>Découvrez les meilleurs exercices pour rester en forme et améliorer votre santé. Le sport est essentiel pour un mode de vie équilibré.</p><p>Intégrer une routine d'exercice régulière peut transformer votre bien-être physique et mental.</p>",
            "<p>Explorez les richesses culturelles et historiques qui façonnent notre monde. La culture est le reflet de notre humanité.</p><p>Chaque culture apporte une perspective unique et enrichissante à notre compréhension du monde.</p>"
        )

        val timestamp = System.currentTimeMillis()
        val createdArticles = mutableListOf<String>()
        var publishedCount = 0
        var privateCount = 0
        var scheduledCount = 0

        val articles = (1..request.count).map { index ->
            async {
                val title = "${request.titlePrefix} ${index}"
                val category = categories[random.nextInt(categories.size)]
                val tags = tagsList[random.nextInt(tagsList.size)]
                val content = contentTemplates[random.nextInt(contentTemplates.size)]
                val excerpt = "Résumé de l'article ${index}: ${content.replace(Regex("<[^>]*>"), " ").take(150)}..."

                // Déterminer si l'article sera publié
                val isPublished = if (request.publishSome) {
                    (random.nextInt(100) < request.publishedPercentage)
                } else {
                    false
                }

                // Déterminer si l'article sera privé
                val isPrivate = if (request.makeSomePrivate) {
                    (random.nextInt(100) < request.privatePercentage)
                } else {
                    false
                }

                // Déterminer si l'article aura une date de publication programmée
                val publishAt: Instant? = if (request.scheduleSome && (random.nextInt(100) < request.scheduledPercentage)) {
                    // Programmer pour dans 1 à 30 jours
                    Instant.now().plusSeconds((1 + random.nextInt(30)) * 24 * 60 * 60L)
                } else {
                    null
                }

                val createRequest = CreateArticleRequest(
                    title = title,
                    content = content,
                    excerpt = excerpt,
                    coverImageUrl = null,
                    tags = tags,
                    category = category,
                    isPublished = isPublished,
                    isPrivate = isPrivate,
                    publishAt = publishAt
                )

                try {
                    val article = createSimpleArticle(createRequest, authorId)
                    createdArticles.add(article.id)
                    if (isPublished) publishedCount++
                    if (isPrivate) privateCount++
                    if (publishAt != null) scheduledCount++
                    logger.info("Created test article: ${article.title} (Published: $isPublished, Private: $isPrivate)")
                    article
                } catch (e: Exception) {
                    logger.warn("Failed to create article $title: ${e.message}")
                    null
                }
            }
        }.awaitAll().filterNotNull()

        mapOf(
            "totalRequested" to request.count,
            "totalCreated" to articles.size,
            "publishedArticles" to publishedCount,
            "privateArticles" to privateCount,
            "scheduledArticles" to scheduledCount,
            "createdArticleIds" to createdArticles,
            "message" to "Batch article creation completed"
        )
    }
}