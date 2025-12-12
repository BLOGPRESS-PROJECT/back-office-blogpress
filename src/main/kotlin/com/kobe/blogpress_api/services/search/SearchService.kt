package com.kobe.blogpress_api.services.search

import com.kobe.blogpress_api.domain.model.article.Article
import com.kobe.blogpress_api.domain.model.blog.Blog
import com.kobe.blogpress_api.dto.search.SearchItemDto
import com.kobe.blogpress_api.dto.search.SearchItemType
import com.kobe.blogpress_api.dto.search.SearchResultDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.text.Normalizer


@Service
class SearchService(
    private val mongoTemplate: ReactiveMongoTemplate,
    @Value("\${app.base-url:http://localhost:8090}") private val baseUrl: String
) {

    /**
     * RECHERCHE PRINCIPALE - Recherche floue intelligente
     * Trouve des résultats même si le titre ne correspond pas exactement
     */
    suspend fun search(
        query: String,
        page: Int = 0,
        size: Int = 20,
        type: SearchItemType? = null
    ): SearchResultDto = coroutineScope {
        val startTime = System.currentTimeMillis()

        val cleanQuery = normalizeQuery(query)

        if (cleanQuery.isBlank() || cleanQuery.length < 2) {
            return@coroutineScope SearchResultDto(
                results = emptyList(),
                totalResults = 0,
                page = page,
                size = size,
                totalPages = 0,
                query = query,
                searchTime = 0
            )
        }

        // Tokeniser la requête en mots individuels
        val searchTokens = tokenizeQuery(cleanQuery)

        // Chercher selon le type demandé
        val results = when (type) {
            SearchItemType.BLOG -> searchOnlyBlogs(searchTokens)
            SearchItemType.ARTICLE -> searchOnlyArticles(searchTokens)
            null -> searchBoth(searchTokens)
        }

        val searchTime = System.currentTimeMillis() - startTime

        // Trier par pertinence décroissante
        val sortedResults = results
            .sortedByDescending { it.relevanceScore }
            .drop(page * size)
            .take(size)

        SearchResultDto(
            results = sortedResults,
            totalResults = results.size.toLong(),
            page = page,
            size = size,
            totalPages = if (results.isEmpty()) 0 else (results.size / size) + 1,
            query = query,
            searchTime = searchTime
        )
    }

    /**
     * Chercher dans les BLOGS et les ARTICLES en parallèle
     */
    private suspend fun searchBoth(tokens: List<String>): List<SearchItemDto> = coroutineScope {
        val blogsDeferred = async { searchOnlyBlogs(tokens) }
        val articlesDeferred = async { searchOnlyArticles(tokens) }

        val blogs = blogsDeferred.await()
        val articles = articlesDeferred.await()

        blogs + articles
    }

    /**
     * Chercher uniquement dans les BLOGS avec recherche floue
     */
    private suspend fun searchOnlyBlogs(tokens: List<String>): List<SearchItemDto> {
        // Construire une recherche qui trouve au moins UN des mots
        val orCriteriaList = mutableListOf<Criteria>()

        tokens.forEach { token ->
            // Pour chaque mot, chercher dans plusieurs champs
            orCriteriaList.add(
                Criteria().orOperator(
                    Criteria.where("title").regex(token, "i"),
                    Criteria.where("description").regex(token, "i"),
                    Criteria.where("tags").regex(token, "i"),
                    // Recherche partielle (préfixe) - trouve "program" dans "programming"
                    Criteria.where("title").regex(".*$token.*", "i"),
                    Criteria.where("description").regex(".*$token.*", "i")
                )
            )
        }

        val criteria = Criteria().andOperator(
            Criteria.where("isPublished").`is`(true),
            Criteria.where("isPrivate").`is`(false),
            Criteria().orOperator(*orCriteriaList.toTypedArray())
        )

        val mongoQuery = Query.query(criteria)
            .with(Sort.by(Sort.Direction.DESC, "createdAt"))
            .limit(100)

        val blogs = mongoTemplate.find(mongoQuery, Blog::class.java)
            .asFlow()
            .toList()

        return blogs.map { blog ->
            SearchItemDto(
                id = blog.id.toHexString(),
                type = SearchItemType.BLOG,
                title = blog.title,
                excerpt = blog.description,
                slug = blog.slug,
                coverImageUrl = buildBlogCoverImageUrl(blog),
                publicUrl = "$baseUrl/blog/${blog.slug}",
                authorId = blog.authorId.toHexString(),
                createdAt = blog.createdAt,
                viewCount = blog.viewCount,
                likeCount = blog.likeCount,
                articleType = null,
                blogId = null,
                blogTitle = null,
                relevanceScore = calculateFuzzyRelevance(
                    tokens,
                    blog.title,
                    blog.description ?: "",
                    blog.tags
                )
            )
        }
    }

    /**
     * Chercher uniquement dans les ARTICLES avec recherche floue
     */
    private suspend fun searchOnlyArticles(tokens: List<String>): List<SearchItemDto> {
        val orCriteriaList = mutableListOf<Criteria>()

        tokens.forEach { token ->
            orCriteriaList.add(
                Criteria().orOperator(
                    Criteria.where("title").regex(token, "i"),
                    Criteria.where("excerpt").regex(token, "i"),
                    Criteria.where("content").regex(token, "i"),
                    Criteria.where("tags").regex(token, "i"),
                    // Recherche partielle
                    Criteria.where("title").regex(".*$token.*", "i"),
                    Criteria.where("excerpt").regex(".*$token.*", "i")
                )
            )
        }

        val criteria = Criteria().andOperator(
            Criteria.where("isPublished").`is`(true),
            Criteria.where("isPrivate").`is`(false),
            Criteria().orOperator(*orCriteriaList.toTypedArray())
        )

        val mongoQuery = Query.query(criteria)
            .with(Sort.by(Sort.Direction.DESC, "createdAt"))
            .limit(100)

        val articles = mongoTemplate.find(mongoQuery, Article::class.java)
            .asFlow()
            .toList()

        return articles.map { article ->
            val blog = if (article.blogId != null) {
                mongoTemplate.findById(article.blogId, Blog::class.java).block()
            } else null

            val publicUrl = if (blog != null) {
                "$baseUrl/blog/${blog.slug}/post/${article.slug}"
            } else {
                "$baseUrl/article/${article.slug}"
            }

            SearchItemDto(
                id = article.id.toHexString(),
                type = SearchItemType.ARTICLE,
                title = article.title,
                excerpt = article.excerpt,
                slug = article.slug,
                coverImageUrl = buildArticleCoverImageUrl(article),
                publicUrl = publicUrl,
                authorId = article.authorId.toHexString(),
                createdAt = article.createdAt,
                viewCount = article.viewCount,
                likeCount = article.likeCount,
                articleType = article.type,
                blogId = article.blogId?.toHexString(),
                blogTitle = blog?.title,
                relevanceScore = calculateFuzzyRelevance(
                    tokens,
                    article.title,
                    article.excerpt ?: article.content.take(200),
                    article.tags
                )
            )
        }
    }

    /**
     * RECHERCHE AVANCÉE avec filtres multiples
     */
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

        val cleanQuery = normalizeQuery(query)
        val tokens = tokenizeQuery(cleanQuery)

        val results = when (type) {
            SearchItemType.BLOG -> advancedSearchBlogs(tokens, category, tags, authorId)
            SearchItemType.ARTICLE -> advancedSearchArticles(tokens, category, tags, authorId)
            null -> {
                val blogs = async { advancedSearchBlogs(tokens, category, tags, authorId) }
                val articles = async { advancedSearchArticles(tokens, category, tags, authorId) }
                blogs.await() + articles.await()
            }
        }

        val searchTime = System.currentTimeMillis() - startTime

        val sortedResults = results
            .sortedByDescending { it.relevanceScore }
            .drop(page * size)
            .take(size)

        SearchResultDto(
            results = sortedResults,
            totalResults = results.size.toLong(),
            page = page,
            size = size,
            totalPages = if (results.isEmpty()) 0 else (results.size / size) + 1,
            query = query,
            searchTime = searchTime
        )
    }

    private suspend fun advancedSearchBlogs(
        tokens: List<String>,
        category: String?,
        tags: List<String>?,
        authorId: String?
    ): List<SearchItemDto> {
        val criteriaList = mutableListOf<Criteria>()

        criteriaList.add(Criteria.where("isPublished").`is`(true))
        criteriaList.add(Criteria.where("isPrivate").`is`(false))

        // Recherche floue sur les tokens
        if (tokens.isNotEmpty()) {
            val orCriteriaList = mutableListOf<Criteria>()
            tokens.forEach { token ->
                orCriteriaList.add(
                    Criteria().orOperator(
                        Criteria.where("title").regex(token, "i"),
                        Criteria.where("description").regex(token, "i"),
                        Criteria.where("tags").regex(token, "i")
                    )
                )
            }
            criteriaList.add(Criteria().orOperator(*orCriteriaList.toTypedArray()))
        }

        if (category != null) {
            criteriaList.add(Criteria.where("category").`is`(category))
        }

        if (!tags.isNullOrEmpty()) {
            criteriaList.add(Criteria.where("tags").`in`(tags))
        }

        if (authorId != null) {
            criteriaList.add(Criteria.where("authorId").`is`(org.bson.types.ObjectId(authorId)))
        }

        val finalCriteria = Criteria().andOperator(*criteriaList.toTypedArray())

        val mongoQuery = Query.query(finalCriteria)
            .with(Sort.by(Sort.Direction.DESC, "createdAt"))
            .limit(100)

        val blogs = mongoTemplate.find(mongoQuery, Blog::class.java)
            .asFlow()
            .toList()

        return blogs.map { blog ->
            SearchItemDto(
                id = blog.id.toHexString(),
                type = SearchItemType.BLOG,
                title = blog.title,
                excerpt = blog.description,
                slug = blog.slug,
                coverImageUrl = buildBlogCoverImageUrl(blog),
                publicUrl = "$baseUrl/blog/${blog.slug}",
                authorId = blog.authorId.toHexString(),
                createdAt = blog.createdAt,
                viewCount = blog.viewCount,
                likeCount = blog.likeCount,
                relevanceScore = calculateFuzzyRelevance(
                    tokens,
                    blog.title,
                    blog.description ?: "",
                    blog.tags
                )
            )
        }
    }

    private suspend fun advancedSearchArticles(
        tokens: List<String>,
        category: String?,
        tags: List<String>?,
        authorId: String?
    ): List<SearchItemDto> {
        val criteriaList = mutableListOf<Criteria>()

        criteriaList.add(Criteria.where("isPublished").`is`(true))
        criteriaList.add(Criteria.where("isPrivate").`is`(false))

        if (tokens.isNotEmpty()) {
            val orCriteriaList = mutableListOf<Criteria>()
            tokens.forEach { token ->
                orCriteriaList.add(
                    Criteria().orOperator(
                        Criteria.where("title").regex(token, "i"),
                        Criteria.where("excerpt").regex(token, "i"),
                        Criteria.where("content").regex(token, "i"),
                        Criteria.where("tags").regex(token, "i")
                    )
                )
            }
            criteriaList.add(Criteria().orOperator(*orCriteriaList.toTypedArray()))
        }

        if (category != null) {
            criteriaList.add(Criteria.where("category").`is`(category))
        }

        if (!tags.isNullOrEmpty()) {
            criteriaList.add(Criteria.where("tags").`in`(tags))
        }

        if (authorId != null) {
            criteriaList.add(Criteria.where("authorId").`is`(org.bson.types.ObjectId(authorId)))
        }

        val finalCriteria = Criteria().andOperator(*criteriaList.toTypedArray())

        val mongoQuery = Query.query(finalCriteria)
            .with(Sort.by(Sort.Direction.DESC, "createdAt"))
            .limit(100)

        val articles = mongoTemplate.find(mongoQuery, Article::class.java)
            .asFlow()
            .toList()

        return articles.map { article ->
            val blog = if (article.blogId != null) {
                mongoTemplate.findById(article.blogId, Blog::class.java).block()
            } else null

            val publicUrl = if (blog != null) {
                "$baseUrl/blog/${blog.slug}/post/${article.slug}"
            } else {
                "$baseUrl/article/${article.slug}"
            }

            SearchItemDto(
                id = article.id.toHexString(),
                type = SearchItemType.ARTICLE,
                title = article.title,
                excerpt = article.excerpt,
                slug = article.slug,
                coverImageUrl = buildArticleCoverImageUrl(article),
                publicUrl = publicUrl,
                authorId = article.authorId.toHexString(),
                createdAt = article.createdAt,
                viewCount = article.viewCount,
                likeCount = article.likeCount,
                articleType = article.type,
                blogId = article.blogId?.toHexString(),
                blogTitle = blog?.title,
                relevanceScore = calculateFuzzyRelevance(
                    tokens,
                    article.title,
                    article.excerpt ?: article.content.take(200),
                    article.tags
                )
            )
        }
    }

    /**
     * Tokeniser la requête en mots individuels
     * Exemple: "Kotlin Spring Boot" -> ["kotlin", "spring", "boot"]
     */
    private fun tokenizeQuery(query: String): List<String> {
        return query
            .lowercase()
            .split(Regex("\\s+")) // Split sur les espaces
            .filter { it.length >= 2 } // Ignorer les mots de 1 lettre
            .distinct() // Supprimer les doublons
    }

    /**
     * Nettoyer et normaliser la requête de recherche
     * - Supprime les accents
     * - Trim les espaces
     */
    private fun normalizeQuery(query: String): String {
        if (query.isBlank()) return ""

        // Supprimer les accents
        val normalized = Normalizer.normalize(query.trim(), Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

        return normalized
    }

    /**
     * Calculer un score de pertinence FLOU
     * Prend en compte la correspondance partielle des mots
     */
    private fun calculateFuzzyRelevance(
        tokens: List<String>,
        title: String,
        content: String,
        tags: List<String> = emptyList()
    ): Double {
        if (tokens.isEmpty()) return 0.0

        val titleLower = title.lowercase()
        val contentLower = content.lowercase()
        val tagsLower = tags.map { it.lowercase() }

        var score = 0.0
        var matchedTokens = 0

        tokens.forEach { token ->
            var tokenMatched = false

            // 1. Correspondance EXACTE du mot complet dans le titre
            if (titleLower.split(Regex("\\s+")).contains(token)) {
                score += 100.0
                tokenMatched = true
            }

            // 2. Le titre COMMENCE par ce mot
            if (titleLower.startsWith(token)) {
                score += 50.0
                tokenMatched = true
            }

            // 3. Le titre CONTIENT ce mot (même partiellement)
            if (titleLower.contains(token)) {
                score += 30.0
                tokenMatched = true
            }

            // 4. Le mot est dans les tags
            if (tagsLower.any { it.contains(token) }) {
                score += 40.0
                tokenMatched = true
            }

            // 5. Le contenu contient ce mot
            if (contentLower.contains(token)) {
                score += 10.0
                tokenMatched = true
            }

            // 6. Bonus pour correspondance partielle (préfixe)
            // Ex: "prog" trouve "programming"
            if (titleLower.split(Regex("\\s+")).any { it.startsWith(token) }) {
                score += 20.0
                tokenMatched = true
            }

            if (tokenMatched) {
                matchedTokens++
            }
        }

        // Bonus si TOUS les mots de la requête sont trouvés
        if (matchedTokens == tokens.size) {
            score += 50.0
        }

        // Bonus proportionnel au pourcentage de mots trouvés
        val matchPercentage = matchedTokens.toDouble() / tokens.size
        score += matchPercentage * 30.0

        return score
    }

    /**
     * Construire l'URL complète de l'image de couverture d'un article
     */
    private fun buildArticleCoverImageUrl(article: Article): String? {
        return if (article.coverImageUrl != null && article.coverImageUrl.isNotBlank()) {
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
    }

    /**
     * Construire l'URL complète de l'image de couverture d'un blog
     */
    private fun buildBlogCoverImageUrl(blog: Blog): String? {
        return if (blog.coverImageUrl != null && blog.coverImageUrl.isNotBlank()) {
            if (blog.coverImageUrl.startsWith("http://") || blog.coverImageUrl.startsWith("https://")) {
                // URL externe complète, l'utiliser telle quelle
                blog.coverImageUrl
            } else {
                // Chemin relatif ou nom de fichier, construire l'URL complète
                "$baseUrl/api/blogs/${blog.id.toHexString()}/cover-image"
            }
        } else {
            null
        }
    }

    /**
     * Obtenir des suggestions de recherche (autocomplete)
     */
    suspend fun getSuggestions(query: String, limit: Int = 5): List<String> {
        if (query.length < 2) return emptyList()

        val cleanQuery = normalizeQuery(query)
        val tokens = tokenizeQuery(cleanQuery)

        if (tokens.isEmpty()) return emptyList()

        // Chercher les titres qui commencent par le dernier mot tapé
        val lastToken = tokens.last()

        val criteria = Criteria().orOperator(
            Criteria.where("title").regex("^$lastToken", "i"),
            Criteria.where("title").regex("\\b$lastToken", "i"), // Début d'un mot
            Criteria.where("tags").regex("^$lastToken", "i")
        )

        val mongoQuery = Query.query(criteria)
            .limit(limit * 2)

        // Chercher dans les blogs
        val blogTitles = mongoTemplate.find(mongoQuery, Blog::class.java)
            .asFlow()
            .toList()
            .map { it.title }

        // Chercher dans les articles
        val articleTitles = mongoTemplate.find(mongoQuery, Article::class.java)
            .asFlow()
            .toList()
            .map { it.title }

        return (blogTitles + articleTitles)
            .distinct()
            .take(limit)
    }
}