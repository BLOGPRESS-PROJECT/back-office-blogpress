package com.kobe.blogpress_api.services.article

import com.kobe.blogpress_api.domain.model.article.Article
import com.kobe.blogpress_api.domain.model.article.ArticleType
import com.kobe.blogpress_api.dto.article.ArticleResponse
import com.kobe.blogpress_api.dto.article.ArticleStats
import com.kobe.blogpress_api.dto.article.ArticleSummaryDto
import com.kobe.blogpress_api.dto.article.CreateArticleRequest
import com.kobe.blogpress_api.dto.article.CreateBlogPostRequest
import com.kobe.blogpress_api.dto.article.UpdateArticleRequest
import com.kobe.blogpress_api.exception.ContentNotYetPublishedException
import com.kobe.blogpress_api.exception.ResourceNotFoundException
import com.kobe.blogpress_api.repository.article.ArticleRepository
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
class ArticleService(
    private val articleRepository: ArticleRepository,
    private val blogRepository: BlogRepository,
    private val articleSlugService: ArticleSlugService,
    @Value("\${app.base-url:http://localhost:8090}") private val baseUrl: String
) {

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
            readTime = readTime
        )

        val savedArticle = articleRepository.save(article).awaitSingle()

        // Incrémenter le compteur d'articles du blog
        val updatedBlog = blog.copy(postCount = blog.postCount + 1)
        blogRepository.save(updatedBlog).awaitSingle()

        return toArticleResponse(savedArticle, blog.slug)
    }

    // ===== LECTURE =====

    suspend fun getArticleBySlug(slug: String, userId: ObjectId? = null): ArticleResponse {
        val article = articleRepository.findBySlug(slug).awaitSingleOrNull()
            ?: throw ResourceNotFoundException("Article not found with slug: $slug")

        checkArticleAccess(article, userId)

        val blogSlug = if (article.blogId != null) {
            blogRepository.findById(article.blogId).awaitSingleOrNull()?.slug
        } else null

        return toArticleResponse(article, blogSlug)
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

        return toArticleResponse(article, blogSlug)
    }

    suspend fun getArticleById(articleId: ObjectId): ArticleResponse {
        val article = findById(articleId)
        val blogSlug = if (article.blogId != null) {
            blogRepository.findById(article.blogId).awaitSingleOrNull()?.slug
        } else null
        return toArticleResponse(article, blogSlug)
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

        val updatedArticle = article.copy(
            title = request.title ?: article.title,
            content = request.content ?: article.content,
            excerpt = request.excerpt ?: article.excerpt,
            slug = newSlug,
            coverImageUrl = request.coverImageUrl ?: article.coverImageUrl,
            tags = request.tags ?: article.tags,
            category = request.category ?: article.category,
            isPublished = finalIsPublished,
            isPrivate = request.isPrivate ?: article.isPrivate,
            publishAt = finalPublishAt,
            readTime = newReadTime,
            updatedAt = Instant.now()
        )

        val savedArticle = articleRepository.save(updatedArticle).awaitSingle()

        val blogSlug = if (savedArticle.blogId != null) {
            blogRepository.findById(savedArticle.blogId).awaitSingleOrNull()?.slug
        } else null

        return toArticleResponse(savedArticle, blogSlug)
    }

    // ===== SUPPRESSION =====

    suspend fun deleteArticle(articleId: ObjectId, authorId: ObjectId) {
        val article = findById(articleId)

        // Vérifier que l'utilisateur est l'auteur
        if (article.authorId != authorId) {
            throw IllegalArgumentException("You are not authorized to delete this article")
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

    private suspend fun toArticleResponse(article: Article, blogSlug: String? = null): ArticleResponse {
        val publicUrl = when (article.type) {
            ArticleType.SIMPLE_ARTICLE -> "$baseUrl/article/${article.slug}"
            ArticleType.BLOG_POST -> {
                val slug = blogSlug ?: blogRepository.findById(article.blogId!!).awaitSingleOrNull()?.slug
                "$baseUrl/blog/$slug/post/${article.slug}"
            }
        }

        return ArticleResponse(
            id = article.id.toHexString(),
            title = article.title,
            content = article.content,
            excerpt = article.excerpt,
            slug = article.slug,
            coverImageUrl = article.coverImageUrl,
            tags = article.tags,
            category = article.category,
            authorId = article.authorId.toHexString(),
            blogId = article.blogId?.toHexString(),
            type = article.type,
            isPublished = article.isPublished,
            isPrivate = article.isPrivate,
            publishAt = article.publishAt,
            publicUrl = publicUrl,
            createdAt = article.createdAt,
            updatedAt = article.updatedAt,
            viewCount = article.viewCount,
            likeCount = article.likeCount,
            commentCount = article.commentCount,
            shareCount = article.shareCount,
            readTime = article.readTime
        )
    }

    private suspend fun toArticleSummaryDto(article: Article): ArticleSummaryDto {
        val publicUrl = when (article.type) {
            ArticleType.SIMPLE_ARTICLE -> "$baseUrl/article/${article.slug}"
            ArticleType.BLOG_POST -> {
                val blogSlug = if (article.blogId != null) {
                    blogRepository.findById(article.blogId).awaitSingleOrNull()?.slug
                } else null
                "$baseUrl/blog/$blogSlug/post/${article.slug}"
            }
        }

        return ArticleSummaryDto(
            id = article.id.toHexString(),
            title = article.title,
            excerpt = article.excerpt,
            slug = article.slug,
            coverImageUrl = article.coverImageUrl,
            tags = article.tags,
            category = article.category,
            authorId = article.authorId.toHexString(),
            blogId = article.blogId?.toHexString(),
            type = article.type,
            isPublished = article.isPublished,
            isPrivate = article.isPrivate,
            publicUrl = publicUrl,
            createdAt = article.createdAt,
            updatedAt = article.updatedAt,
            readTime = article.readTime,
            stats = ArticleStats(
                viewCount = article.viewCount,
                likeCount = article.likeCount,
                commentCount = article.commentCount,
                shareCount = article.shareCount
            )
        )
    }
}