package com.kobe.blogpress_api.dto.analytics

import com.kobe.blogpress_api.domain.model.analytics.ContentAnalyticsType
import java.time.Instant

data class ContentAnalyticsDTO(
    val contentId: String,
    val contentType: ContentAnalyticsType,
    val views: Long,
    val uniqueVisitors: Long,
    val averageTimeOnPage: Double,
    val bounceRate: Double,
    val likes: Long,
    val comments: Long,
    val shares: Long,
    val favorites: Long,
    val engagementRate: Double,
    val trafficSources: Map<String, Long>,
    val topReferrers: Map<String, Long>,
    val viewsByCountry: Map<String, Long>,
    val viewsByHour: Map<Int, Long>,
    val viewsByDayOfWeek: Map<Int, Long>,
    val periodStart: Instant,
    val periodEnd: Instant,
    val lastUpdated: Instant
)

data class UserAnalyticsSummaryDTO(
    val userId: String,
    val totalViews: Long,
    val totalUniqueVisitors: Long,
    val totalLikes: Long,
    val totalComments: Long,
    val totalShares: Long,
    val averageEngagementRate: Double,
    val topContent: List<ContentAnalyticsSummaryDTO>,
    val trafficSources: Map<String, Long>,
    val topCountries: Map<String, Long>,
    val periodStart: Instant,
    val periodEnd: Instant
)

data class ContentAnalyticsSummaryDTO(
    val contentId: String,
    val contentType: ContentAnalyticsType,
    val title: String,
    val views: Long,
    val likes: Long,
    val engagementRate: Double
)

