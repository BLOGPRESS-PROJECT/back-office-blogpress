package com.kobe.blogpress_api.controller.golden

import com.kobe.blogpress_api.domain.model.analytics.ContentAnalyticsType
import com.kobe.blogpress_api.dto.analytics.ContentAnalyticsDTO
import com.kobe.blogpress_api.dto.analytics.ContentAnalyticsSummaryDTO
import com.kobe.blogpress_api.dto.analytics.UserAnalyticsSummaryDTO
import com.kobe.blogpress_api.dto.common.ApiResponseDto
import com.kobe.blogpress_api.services.analytics.AnalyticsService
import kotlinx.coroutines.reactor.mono
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.util.*

/**
 * Contrôleur pour les analytics avancés (Golden Users uniquement).
 */
@RestController
@RequestMapping("/api/analytics")
class AnalyticsController(
    private val analyticsService: AnalyticsService
) {
    private val logger = LoggerFactory.getLogger(AnalyticsController::class.java)

    /**
     * Récupère les analytics pour un contenu spécifique (blog ou article).
     * GET /api/analytics/content/{contentId}?type=BLOG|ARTICLE&periodDays=30
     */
    @GetMapping("/content/{contentId}")
    fun getContentAnalytics(
        @AuthenticationPrincipal userId: String,
        @PathVariable contentId: String,
        @RequestParam type: ContentAnalyticsType,
        @RequestParam(defaultValue = "30") periodDays: Int
    ): Mono<ResponseEntity<ApiResponseDto<ContentAnalyticsDTO>>> = mono {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get analytics for content: $contentId (type: $type) by user: $userId")

        try {
            val analytics = analyticsService.getContentAnalytics(
                ObjectId(userId),
                ObjectId(contentId),
                type,
                periodDays
            )

            val analyticsDTO = ContentAnalyticsDTO(
                contentId = analytics.contentId.toHexString(),
                contentType = analytics.contentType,
                views = analytics.views,
                uniqueVisitors = analytics.uniqueVisitors,
                averageTimeOnPage = analytics.averageTimeOnPage,
                bounceRate = analytics.bounceRate,
                likes = analytics.likes,
                comments = analytics.comments,
                shares = analytics.shares,
                favorites = analytics.favorites,
                engagementRate = analytics.engagementRate,
                trafficSources = analytics.trafficSources,
                topReferrers = analytics.topReferrers,
                viewsByCountry = analytics.viewsByCountry,
                viewsByHour = analytics.viewsByHour,
                viewsByDayOfWeek = analytics.viewsByDayOfWeek,
                periodStart = analytics.periodStart,
                periodEnd = analytics.periodEnd,
                lastUpdated = analytics.lastUpdated
            )

            ResponseEntity.ok(
                ApiResponseDto.success(
                    data = analyticsDTO,
                    message = "Analytics retrieved successfully",
                    requestId = requestId
                )
            )
        } catch (e: IllegalAccessException) {
            logger.warn("[$requestId] Access denied for analytics: ${e.message}")
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponseDto.error(
                    message = "Advanced analytics are only available for Golden Users",
                    requestId = requestId
                ))
        } catch (e: Exception) {
            logger.error("[$requestId] Error retrieving analytics for content: $contentId", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error(
                    message = "Error retrieving analytics",
                    requestId = requestId
                ))
        }
    }

    /**
     * Récupère le résumé des analytics pour l'utilisateur connecté.
     * GET /api/analytics/summary?periodDays=30
     */
    @GetMapping("/summary")
    fun getUserAnalyticsSummary(
        @AuthenticationPrincipal userId: String,
        @RequestParam(defaultValue = "30") periodDays: Int
    ): Mono<ResponseEntity<ApiResponseDto<UserAnalyticsSummaryDTO>>> = mono {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get analytics summary for user: $userId")

        try {
            val summary = analyticsService.getUserAnalyticsSummary(ObjectId(userId), periodDays)

            val summaryDTO = UserAnalyticsSummaryDTO(
                userId = summary.userId.toHexString(),
                totalViews = summary.totalViews,
                totalUniqueVisitors = summary.totalUniqueVisitors,
                totalLikes = summary.totalLikes,
                totalComments = summary.totalComments,
                totalShares = summary.totalShares,
                averageEngagementRate = summary.averageEngagementRate,
                topContent = summary.topContent.map { content ->
                    ContentAnalyticsSummaryDTO(
                        contentId = content.contentId.toHexString(),
                        contentType = content.contentType,
                        title = content.title,
                        views = content.views,
                        likes = content.likes,
                        engagementRate = content.engagementRate
                    )
                },
                trafficSources = summary.trafficSources,
                topCountries = summary.topCountries,
                periodStart = summary.periodStart,
                periodEnd = summary.periodEnd
            )

            ResponseEntity.ok(
                ApiResponseDto.success(
                    data = summaryDTO,
                    message = "Analytics summary retrieved successfully",
                    requestId = requestId
                )
            )
        } catch (e: IllegalAccessException) {
            logger.warn("[$requestId] Access denied for analytics summary: ${e.message}")
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponseDto.error(
                    message = "Advanced analytics are only available for Golden Users",
                    requestId = requestId
                ))
        } catch (e: Exception) {
            logger.error("[$requestId] Error retrieving analytics summary for user: $userId", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error(
                    message = "Error retrieving analytics summary",
                    requestId = requestId
                ))
        }
    }
}

