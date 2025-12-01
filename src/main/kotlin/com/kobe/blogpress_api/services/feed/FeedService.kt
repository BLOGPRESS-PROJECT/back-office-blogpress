package com.kobe.blogpress_api.services.feed

import com.kobe.blogpress_api.domain.interaction.ContentType
import com.kobe.blogpress_api.domain.model.article.Article
import com.kobe.blogpress_api.domain.model.article.ArticleType
import com.kobe.blogpress_api.domain.model.blog.Blog
import com.kobe.blogpress_api.domain.model.user.User
import com.kobe.blogpress_api.dto.feed.FeedItemDto
import com.kobe.blogpress_api.dto.feed.FeedResponse
import com.kobe.blogpress_api.repository.article.ArticleRepository
import com.kobe.blogpress_api.repository.blog.BlogRepository
import com.kobe.blogpress_api.repository.interaction.FavoriteRepository
import com.kobe.blogpress_api.repository.interaction.LikeRepository
import com.kobe.blogpress_api.repository.user.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class FeedService(
    private val articleRepository: ArticleRepository,
    private val blogRepository: BlogRepository,
    private val userRepository: UserRepository,
    private val likeRepository: LikeRepository,
    private val favoriteRepository: FavoriteRepository,
    private val mongoTemplate: ReactiveMongoTemplate,
    @Value("\${app.frontend-url:http://localhost:3000}") private val frontendUrl: String
) {

    private val logger = LoggerFactory.getLogger(FeedService::class.java)

    suspend fun getFeed(
        page: Int = 0,
        size: Int = 20,
        sort: String = "createdAt,desc",
        category: String? = null,
        author: String? = null,
        tags: List<String>? = null,
        type: ArticleType? = null,
        search: String? = null,
        userId: ObjectId? = null // Utilisateur connecté (optionnel)
    ): FeedResponse = coroutineScope {
        logger.info("Getting feed - page: $page, size: $size, sort: $sort, userId: $userId")

        // Construire la requête avec filtres
        val criteria = buildCriteria(
            category = category,
            author = author,
            tags = tags,
            type = type,
            search = search
        )

        // Parser le tri
        val sortDirection = parseSortDirection(sort)
        val sortField = parseSortField(sort)

        // Créer la requête MongoDB
        val query = Query.query(criteria)
            .with(PageRequest.of(page, size, Sort.by(sortDirection, sortField)))

        // Compter le total d'éléments
        val totalElements = mongoTemplate.count(Query.query(criteria), Article::class.java).awaitSingle()

        // Récupérer les articles
        val articles = mongoTemplate.find(query, Article::class.java)
            .asFlow()
            .toList()

        logger.info("Found ${articles.size} articles (total: $totalElements)")

        // Récupérer les informations nécessaires pour le mapping
        val feedItems = articles.map { article ->
            async {
                mapToFeedItemDto(article, userId)
            }
        }.awaitAll()

        // Calculer les métadonnées de pagination
        val totalPages = if (totalElements == 0L) 0 else ((totalElements - 1) / size + 1).toInt()
        val hasNext = page < totalPages - 1
        val hasPrevious = page > 0

        FeedResponse(
            content = feedItems,
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages,
            hasNext = hasNext,
            hasPrevious = hasPrevious,
            isFirst = page == 0,
            isLast = !hasNext
        )
    }

    private fun buildCriteria(
        category: String?,
        author: String?,
        tags: List<String>?,
        type: ArticleType?,
        search: String?
    ): Criteria {
        val criteria = Criteria.where("isPublished").`is`(true)
            .and("isPrivate").`is`(false)

        // Filtrer par type
        if (type != null) {
            criteria.and("type").`is`(type)
        }

        // Filtrer par catégorie
        if (!category.isNullOrBlank()) {
            criteria.and("category").`is`(category)
        }

        // Filtrer par auteur
        if (!author.isNullOrBlank()) {
            try {
                val authorId = ObjectId(author)
                criteria.and("authorId").`is`(authorId)
            } catch (e: IllegalArgumentException) {
                logger.warn("Invalid author ID format: $author")
            }
        }

        // Filtrer par tags
        if (!tags.isNullOrEmpty()) {
            criteria.and("tags").`in`(tags)
        }

        // Recherche par titre/contenu
        if (!search.isNullOrBlank()) {
            val searchCriteria = Criteria().orOperator(
                Criteria.where("title").regex(search, "i"),
                Criteria.where("content").regex(search, "i"),
                Criteria.where("excerpt").regex(search, "i")
            )
            criteria.andOperator(searchCriteria)
        }

        return criteria
    }

    private fun parseSortDirection(sort: String): Sort.Direction {
        val parts = sort.split(",")
        return when (parts.getOrNull(1)?.lowercase()) {
            "asc" -> Sort.Direction.ASC
            "desc" -> Sort.Direction.DESC
            else -> Sort.Direction.DESC
        }
    }

    private fun parseSortField(sort: String): String {
        val parts = sort.split(",")
        return parts.getOrNull(0) ?: "createdAt"
    }

    private suspend fun mapToFeedItemDto(
        article: Article,
        userId: ObjectId? = null
    ): FeedItemDto = coroutineScope {
        // Récupérer l'auteur
        val author = userRepository.findById(article.authorId).awaitSingle()

        // Récupérer le blog si c'est un BLOG_POST
        val blog = article.blogId?.let { blogId ->
            blogRepository.findById(blogId).awaitSingleOrNull()
        }

        // Construire l'URL relative
        val url = buildRelativeUrl(article, blog)

        // Récupérer les états utilisateur si userId est fourni
        val (isLiked, isFavorited, isFollowingAuthor) = if (userId != null) {
            val likedDeferred = async {
                likeRepository.existsByContentIdAndUserIdAndContentType(
                    article.id,
                    userId,
                    ContentType.ARTICLE
                ).awaitSingle()
            }
            val favoritedDeferred = async {
                favoriteRepository.existsByContentIdAndUserIdAndContentType(
                    article.id,
                    userId,
                    ContentType.ARTICLE
                ).awaitSingle()
            }
            val followingDeferred = async {
                val user = userRepository.findById(userId).awaitSingle()
                user.following.contains(article.authorId)
            }

            Triple(
                likedDeferred.await(),
                favoritedDeferred.await(),
                followingDeferred.await()
            )
        } else {
            Triple(false, false, false)
        }

        // Construire l'excerpt si manquant
        val excerpt = article.excerpt ?: extractExcerpt(article.content)

        FeedItemDto(
            id = article.id.toHexString(),
            blogId = article.blogId?.toHexString(),
            blogTitle = blog?.title,
            shareId = article.shareId.toString(),
            publicUrl = article.publicUrl,
            title = article.title,
            excerpt = excerpt,
            coverImageUrl = article.coverImageUrl,
            createdAt = LocalDateTime.ofInstant(article.createdAt, ZoneId.systemDefault()),
            url = url,
            authorName = author.fullName,
            authorAvatar = author.profilePicture,
            authorId = author.id.toHexString(),
            category = article.category ?: "Uncategorized",
            tags = article.tags,
            commentCount = article.commentCount,
            readTime = article.readTime,
            likeCount = article.likeCount,
            viewCount = article.viewCount,
            shareCount = article.shareCount,
            isLiked = isLiked,
            isFavorited = isFavorited,
            isFollowingAuthor = isFollowingAuthor,
            type = article.type,
            isPublished = article.isPublished,
            isPrivate = article.isPrivate
        )
    }

    private fun buildRelativeUrl(article: Article, blog: Blog?): String {
        return when (article.type) {
            ArticleType.BLOG_POST -> {
                // Utiliser shareId pour une meilleure sécurité
                val blogShareId = blog?.shareId ?: article.blogId?.toHexString() ?: ""
                "/blog/$blogShareId/post/${article.shareId}"
            }
            ArticleType.SIMPLE_ARTICLE -> {
                // Utiliser shareId pour une meilleure sécurité
                "/article/share/${article.shareId}"
            }
        }
    }

    private fun extractExcerpt(content: String, maxLength: Int = 200): String {
        // Supprimer les balises HTML
        val textContent = content.replace(Regex("<[^>]*>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return if (textContent.length <= maxLength) {
            textContent
        } else {
            textContent.take(maxLength).trim() + "..."
        }
    }
}

