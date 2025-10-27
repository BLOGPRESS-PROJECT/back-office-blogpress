package com.kobe.blogpress_api.services.search

import com.kobe.blogpress_api.domain.model.article.Article
import com.kobe.blogpress_api.domain.model.blog.Blog
import com.kobe.blogpress_api.dto.search.SearchItemDto
import com.kobe.blogpress_api.dto.search.SearchItemType
import com.kobe.blogpress_api.dto.search.SearchResultDto
import com.kobe.blogpress_api.repository.article.ArticleRepository
import com.kobe.blogpress_api.repository.blog.BlogRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.CriteriaDefinition
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.TextCriteria
import org.springframework.data.mongodb.core.query.TextQuery
import org.springframework.stereotype.Service

@Service
class SearchService(
    private val blogRepository: BlogRepository,
    private val articleRepository: ArticleRepository,
    private val mongoTemplate: ReactiveMongoTemplate,
    @Value("\${app.base-url:http://localhost:8090}") private val baseUrl: String
) {

    suspend fun search(
        query: String,
        page: Int = 0,
        size: Int = 20,
        type: SearchItemType? = null
    ): SearchResultDto = coroutineScope {
        val startTime = System.currentTimeMillis()

        val results = when (type) {
            SearchItemType.BLOG -> searchBlogs(query, page, size)
            SearchItemType.ARTICLE -> searchArticles(query, page, size)
            null -> searchAll(query, page, size)
        }

        val searchTime = System.currentTimeMillis() - startTime

        SearchResultDto(
            results = results,
            totalResults = results.size.toLong(),
            page = page,
            size = size,
            totalPages = (results.size / size) + 1,
            query = query,
            searchTime = searchTime
        )
    }

    private suspend fun searchAll(query: String, page: Int, size: Int): List<SearchItemDto> = coroutineScope {
        // Lancer les recherches en parallèle
        val blogsDeferred = async { searchBlogs(query, 0, size) }
        val articlesDeferred = async { searchArticles(query, 0, size) }

        val blogs = blogsDeferred.await()
        val articles = articlesDeferred.await()

        // Combiner et trier par pertinence
        (blogs + articles)
            .sortedByDescending { it.relevanceScore }
            .drop(page * size)
            .take(size)
    }

    private suspend fun searchBlogs(query: String, page: Int, size: Int): List<SearchItemDto> {
        // Recherche full-text avec MongoDB
        val textCriteria = TextCriteria.forDefaultLanguage()
            .matchingAny(*query.split(" ").toTypedArray())

        val textQuery = TextQuery.queryText(textCriteria)
            .sortByScore()
            .with(PageRequest.of(page, size))

        // Ajouter critère isPublished et isPrivate
        textQuery.addCriteria(Criteria.where("isPublished").`is`(true))
        textQuery.addCriteria(Criteria.where("isPrivate").`is`(false))

        val blogs = mongoTemplate.find(textQuery, Blog::class.java)
            .asFlow()
            .toList()

        return blogs.map { blog ->
            SearchItemDto(
                id = blog.id.toHexString(),
                type = SearchItemType.BLOG,
                title = blog.title,
                excerpt = blog.description,
                slug = blog.slug,
                coverImageUrl = blog.coverImageUrl,
                publicUrl = "$baseUrl/blog/${blog.slug}",
                authorId = blog.authorId.toHexString(),
                createdAt = blog.createdAt,
                viewCount = blog.viewCount,
                likeCount = blog.likeCount,
                relevanceScore = calculateRelevance(query, blog.title, blog.description ?: "")
            )
        }
    }

    private suspend fun searchArticles(query: String, page: Int, size: Int): List<SearchItemDto> {
        val textCriteria = TextCriteria.forDefaultLanguage()
            .matchingAny(*query.split(" ").toTypedArray())

        val textQuery = TextQuery.queryText(textCriteria)
            .sortByScore()
            .with(PageRequest.of(page, size))

        textQuery.addCriteria(Criteria.where("isPublished").`is`(true))
        textQuery.addCriteria(Criteria.where("isPrivate").`is`(false))

        val articles = mongoTemplate.find(textQuery, Article::class.java)
            .asFlow()
            .toList()

        return articles.map { article ->
            val publicUrl = if (article.blogId != null) {
                // Récupérer le slug du blog (optimisation possible avec cache)
                val blog = blogRepository.findById(article.blogId).block()
                "$baseUrl/blog/${blog?.slug}/post/${article.slug}"
            } else {
                "$baseUrl/article/${article.slug}"
            }

            SearchItemDto(
                id = article.id.toHexString(),
                type = SearchItemType.ARTICLE,
                title = article.title,
                excerpt = article.excerpt,
                slug = article.slug,
                coverImageUrl = article.coverImageUrl,
                publicUrl = publicUrl,
                authorId = article.authorId.toHexString(),
                createdAt = article.createdAt,
                viewCount = article.viewCount,
                likeCount = article.likeCount,
                articleType = article.type,
                blogId = article.blogId?.toHexString(),
                relevanceScore = calculateRelevance(
                    query,
                    article.title,
                    article.excerpt ?: article.content.take(200)
                )
            )
        }
    }

    // Calculer un score de pertinence basique
    private fun calculateRelevance(query: String, title: String, content: String): Double {
        val queryLower = query.lowercase()
        val titleLower = title.lowercase()
        val contentLower = content.lowercase()

        var score = 0.0

        // Correspondance exacte dans le titre = score max
        if (titleLower.contains(queryLower)) {
            score += 10.0
        }

        // Correspondance des mots individuels
        queryLower.split(" ").forEach { word ->
            if (word.length > 2) {
                if (titleLower.contains(word)) score += 5.0
                if (contentLower.contains(word)) score += 1.0
            }
        }

        return score
    }

    // Recherche avancée avec filtres
    suspend fun advancedSearch(
        query: String,
        type: SearchItemType? = null,
        category: String? = null,
        tags: List<String>? = null,
        authorId: String? = null,
        page: Int = 0,
        size: Int = 20
    ): SearchResultDto = coroutineScope {
        val startTime = System.currentTimeMillis()

        val mongoQuery = Query()

        // Recherche textuelle
        if (query.isNotBlank()) {
            val textCriteria = TextCriteria.forDefaultLanguage()
                .matchingAny(*query.split(" ").toTypedArray())
            mongoQuery.addCriteria(textCriteria.criteriaObject as CriteriaDefinition)
        }

        // Filtres supplémentaires
        mongoQuery.addCriteria(Criteria.where("isPublished").`is`(true))
        mongoQuery.addCriteria(Criteria.where("isPrivate").`is`(false))

        if (category != null) {
            mongoQuery.addCriteria(Criteria.where("category").`is`(category))
        }

        if (tags != null && tags.isNotEmpty()) {
            mongoQuery.addCriteria(Criteria.where("tags").`in`(tags))
        }

        if (authorId != null) {
            mongoQuery.addCriteria(Criteria.where("authorId").`is`(ObjectId(authorId)))
        }

        mongoQuery.with(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))

        val results = when (type) {
            SearchItemType.BLOG -> {
                mongoTemplate.find(mongoQuery, Blog::class.java)
                    .asFlow()
                    .toList()
                    .map { blogToSearchItem(it) }
            }
            SearchItemType.ARTICLE -> {
                mongoTemplate.find(mongoQuery, Article::class.java)
                    .asFlow()
                    .toList()
                    .map { articleToSearchItem(it) }
            }
            null -> {
                val blogs = mongoTemplate.find(mongoQuery, Blog::class.java)
                    .asFlow()
                    .toList()
                    .map { blogToSearchItem(it) }

                val articles = mongoTemplate.find(mongoQuery, Article::class.java)
                    .asFlow()
                    .toList()
                    .map { articleToSearchItem(it) }

                (blogs + articles).sortedByDescending { it.createdAt }
            }
        }

        val searchTime = System.currentTimeMillis() - startTime

        SearchResultDto(
            results = results,
            totalResults = results.size.toLong(),
            page = page,
            size = size,
            totalPages = (results.size / size) + 1,
            query = query,
            searchTime = searchTime
        )
    }

    private fun blogToSearchItem(blog: Blog) = SearchItemDto(
        id = blog.id.toHexString(),
        type = SearchItemType.BLOG,
        title = blog.title,
        excerpt = blog.description,
        slug = blog.slug,
        coverImageUrl = blog.coverImageUrl,
        publicUrl = "$baseUrl/blog/${blog.slug}",
        authorId = blog.authorId.toHexString(),
        createdAt = blog.createdAt,
        viewCount = blog.viewCount,
        likeCount = blog.likeCount,
        relevanceScore = 0.0
    )

    private fun articleToSearchItem(article: Article): SearchItemDto {
        val publicUrl = if (article.blogId != null) {
            val blog = blogRepository.findById(article.blogId).block()
            "$baseUrl/blog/${blog?.slug}/post/${article.slug}"
        } else {
            "$baseUrl/article/${article.slug}"
        }

        return SearchItemDto(
            id = article.id.toHexString(),
            type = SearchItemType.ARTICLE,
            title = article.title,
            excerpt = article.excerpt,
            slug = article.slug,
            coverImageUrl = article.coverImageUrl,
            publicUrl = publicUrl,
            authorId = article.authorId.toHexString(),
            createdAt = article.createdAt,
            viewCount = article.viewCount,
            likeCount = article.likeCount,
            articleType = article.type,
            blogId = article.blogId?.toHexString(),
            relevanceScore = 0.0
        )
    }
}