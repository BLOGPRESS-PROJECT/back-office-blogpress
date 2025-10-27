package com.kobe.blogpress_api.controller.search

import com.kobe.blogpress_api.dto.common.ApiResponseDto
import com.kobe.blogpress_api.dto.search.SearchItemType
import com.kobe.blogpress_api.dto.search.SearchResultDto
import com.kobe.blogpress_api.services.search.SearchService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/search")
class SearchController(
    private val searchService: SearchService
) {

    private val logger = LoggerFactory.getLogger(SearchController::class.java)

    /**
     * RECHERCHE PRINCIPALE
     * Cherche automatiquement dans les blogs ET les articles
     *
     * Exemples:
     * - /api/search?q=kotlin
     * - /api/search?q=spring boot&page=0&size=10
     * - /api/search?q=programming&type=BLOG
     */
    @GetMapping
    suspend fun search(
        @RequestParam(required = true) q: String,
        @RequestParam(required = false) type: SearchItemType?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponseDto<SearchResultDto>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Search request: query='$q', type=$type, page=$page, size=$size")

        // Validation
        if (q.isBlank()) {
            return ResponseEntity.badRequest().body(
                ApiResponseDto.error(
                    message = "Search query cannot be empty",
                    errorCode = "EMPTY_QUERY"
                )
            )
        }

        if (q.length < 2) {
            return ResponseEntity.badRequest().body(
                ApiResponseDto.error(
                    message = "Search query must be at least 2 characters",
                    errorCode = "QUERY_TOO_SHORT"
                )
            )
        }

        if (q.length > 200) {
            return ResponseEntity.badRequest().body(
                ApiResponseDto.error(
                    message = "Search query is too long (max 200 characters)",
                    errorCode = "QUERY_TOO_LONG"
                )
            )
        }

        // Recherche
        val results = searchService.search(q, page, size, type)

        val searchTypeDesc = when (type) {
            SearchItemType.BLOG -> "in blogs"
            SearchItemType.ARTICLE -> "in articles"
            null -> "in blogs and articles"
        }

        logger.info(
            "[$requestId] Search completed: found ${results.totalResults} results $searchTypeDesc in ${results.searchTime}ms"
        )

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = results,
                message = "Search completed successfully",
                requestId = requestId
            )
        )
    }

    /**
     * RECHERCHE AVANCÉE avec filtres
     *
     * Exemples:
     * - /api/search/advanced?q=kotlin&category=Programming&tags=tutorial,beginners
     * - /api/search/advanced?q=react&type=ARTICLE&authorId=123456
     */
    @GetMapping("/advanced")
    suspend fun advancedSearch(
        @RequestParam(required = true) q: String,
        @RequestParam(required = false) type: SearchItemType?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) tags: List<String>?,
        @RequestParam(required = false) authorId: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponseDto<SearchResultDto>> {
        val requestId = UUID.randomUUID().toString()
        logger.info(
            "[$requestId] Advanced search: query='$q', type=$type, category=$category, tags=$tags, authorId=$authorId"
        )

        // Validation
        if (q.isBlank() || q.length < 2) {
            return ResponseEntity.badRequest().body(
                ApiResponseDto.error(
                    message = "Search query must be at least 2 characters",
                    errorCode = "INVALID_QUERY"
                )
            )
        }

        // Recherche avancée
        val results = searchService.advancedSearch(
            query = q,
            type = type,
            category = category,
            tags = tags,
            authorId = authorId,
            page = page,
            size = size
        )

        logger.info(
            "[$requestId] Advanced search completed: found ${results.totalResults} results in ${results.searchTime}ms"
        )

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = results,
                message = "Advanced search completed successfully",
                requestId = requestId
            )
        )
    }

    /**
     * SUGGESTIONS / AUTOCOMPLETE
     * Retourne des suggestions basées sur les premiers caractères
     *
     * Exemple: /api/search/suggestions?q=kot&limit=5
     */
    @GetMapping("/suggestions")
    suspend fun getSuggestions(
        @RequestParam(required = true) q: String,
        @RequestParam(defaultValue = "5") limit: Int
    ): ResponseEntity<ApiResponseDto<List<String>>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get suggestions for: '$q'")

        if (q.length < 2) {
            return ResponseEntity.badRequest().body(
                ApiResponseDto.error(
                    message = "Query must be at least 2 characters for suggestions",
                    errorCode = "QUERY_TOO_SHORT"
                )
            )
        }

        val suggestions = searchService.getSuggestions(q, limit.coerceIn(1, 10))

        logger.info("[$requestId] Found ${suggestions.size} suggestions")

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = suggestions,
                message = "Suggestions retrieved successfully",
                requestId = requestId
            )
        )
    }
}