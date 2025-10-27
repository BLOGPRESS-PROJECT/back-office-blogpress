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

    @GetMapping
    suspend fun search(
        @RequestParam(required = true) q: String,
        @RequestParam(required = false) type: SearchItemType?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponseDto<SearchResultDto>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Search query: '$q', type: $type, page: $page, size: $size")

        if (q.isBlank() || q.length < 2) {
            return ResponseEntity.badRequest().body(
                ApiResponseDto.error(
                    message = "Search query must be at least 2 characters",
                    errorCode = "INVALID_SEARCH_QUERY"
                )
            )
        }

        val results = searchService.search(q, page, size, type)

        logger.info("[$requestId] Found ${results.totalResults} results in ${results.searchTime}ms")
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = results,
                message = "Search completed successfully",
                requestId = requestId
            )
        )
    }

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
        logger.info("[$requestId] Advanced search: '$q'")

        if (q.isBlank() || q.length < 2) {
            return ResponseEntity.badRequest().body(
                ApiResponseDto.error(
                    message = "Search query must be at least 2 characters",
                    errorCode = "INVALID_SEARCH_QUERY"
                )
            )
        }

        val results = searchService.advancedSearch(
            query = q,
            type = type,
            category = category,
            tags = tags,
            authorId = authorId,
            page = page,
            size = size
        )

        logger.info("[$requestId] Advanced search found ${results.totalResults} results in ${results.searchTime}ms")
        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = results,
                message = "Advanced search completed successfully",
                requestId = requestId
            )
        )
    }
}