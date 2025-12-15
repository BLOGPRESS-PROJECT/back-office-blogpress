package com.kobe.blogpress_api.domain.model.analytics

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * Analytics avancés pour Golden Users.
 * Stocke des métriques détaillées sur les contenus et l'audience.
 */
@Document(collection = "advanced_analytics")
data class AdvancedAnalytics(
    @Id
    val id: ObjectId = ObjectId(),

    @Indexed
    val userId: ObjectId,

    @Indexed
    val contentId: ObjectId, // ID du blog ou article
    val contentType: ContentAnalyticsType, // BLOG ou ARTICLE

    // Métriques de trafic
    val views: Long = 0,
    val uniqueVisitors: Long = 0,
    val averageTimeOnPage: Double = 0.0, // En secondes
    val bounceRate: Double = 0.0, // Pourcentage

    // Métriques d'engagement
    val likes: Long = 0,
    val comments: Long = 0,
    val shares: Long = 0,
    val favorites: Long = 0,
    val engagementRate: Double = 0.0, // Pourcentage

    // Métriques de trafic par source
    val trafficSources: Map<String, Long> = emptyMap(), // "direct", "social", "search", "referral"
    val topReferrers: Map<String, Long> = emptyMap(), // Domaines référents

    // Métriques géographiques
    val viewsByCountry: Map<String, Long> = emptyMap(), // Code pays -> nombre de vues

    // Métriques temporelles
    val viewsByHour: Map<Int, Long> = emptyMap(), // Heure (0-23) -> nombre de vues
    val viewsByDayOfWeek: Map<Int, Long> = emptyMap(), // Jour (0-6) -> nombre de vues

    // Période d'analyse
    val periodStart: Instant,
    val periodEnd: Instant,
    val lastUpdated: Instant = Instant.now()
)

enum class ContentAnalyticsType {
    BLOG,
    ARTICLE
}

/**
 * Analytics agrégés pour un utilisateur (vue d'ensemble).
 */
data class UserAnalyticsSummary(
    val userId: ObjectId,
    val totalViews: Long,
    val totalUniqueVisitors: Long,
    val totalLikes: Long,
    val totalComments: Long,
    val totalShares: Long,
    val averageEngagementRate: Double,
    val topContent: List<ContentAnalyticsSummary>,
    val trafficSources: Map<String, Long>,
    val topCountries: Map<String, Long>,
    val periodStart: Instant,
    val periodEnd: Instant
)

data class ContentAnalyticsSummary(
    val contentId: ObjectId,
    val contentType: ContentAnalyticsType,
    val title: String,
    val views: Long,
    val likes: Long,
    val engagementRate: Double
)

