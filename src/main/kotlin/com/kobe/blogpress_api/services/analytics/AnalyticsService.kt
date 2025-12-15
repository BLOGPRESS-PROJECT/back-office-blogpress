package com.kobe.blogpress_api.services.analytics

import com.kobe.blogpress_api.domain.model.analytics.AdvancedAnalytics
import com.kobe.blogpress_api.domain.model.analytics.ContentAnalyticsSummary
import com.kobe.blogpress_api.domain.model.analytics.ContentAnalyticsType
import com.kobe.blogpress_api.domain.model.analytics.UserAnalyticsSummary
import com.kobe.blogpress_api.repository.analytics.AnalyticsRepository
import com.kobe.blogpress_api.repository.article.ArticleRepository
import com.kobe.blogpress_api.repository.blog.BlogRepository
import com.kobe.blogpress_api.services.user.UserService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Service pour gérer les analytics avancés (Golden Users uniquement).
 */
@Service
class AnalyticsService(
    private val analyticsRepository: AnalyticsRepository,
    private val blogRepository: BlogRepository,
    private val articleRepository: ArticleRepository,
    private val userService: UserService
) {
    private val logger = LoggerFactory.getLogger(AnalyticsService::class.java)

    /**
     * Vérifie si un utilisateur est Golden et peut accéder aux analytics avancés.
     */
    suspend fun canAccessAdvancedAnalytics(userId: ObjectId): Boolean {
        val user = userService.findById(userId)
        return user.isGoldenUser
    }

    /**
     * Récupère les analytics avancés pour un contenu spécifique (blog ou article).
     */
    suspend fun getContentAnalytics(
        userId: ObjectId,
        contentId: ObjectId,
        contentType: ContentAnalyticsType,
        periodDays: Int = 30
    ): AdvancedAnalytics {
        if (!canAccessAdvancedAnalytics(userId)) {
            throw IllegalAccessException("Advanced analytics are only available for Golden Users")
        }

        val periodEnd = Instant.now()
        val periodStart = periodEnd.minus(periodDays.toLong(), ChronoUnit.DAYS)

        val existing = analyticsRepository.findByUserIdAndContentIdAndContentType(userId, contentId, contentType)
            .awaitSingleOrNull()
        
        return if (existing != null && (existing.periodStart.isAfter(periodStart) || existing.periodStart.isBefore(periodStart))) {
            existing
        } else {
            createDefaultAnalytics(userId, contentId, contentType, periodStart, periodEnd)
        }
    }

    /**
     * Récupère le résumé des analytics pour un utilisateur (tous ses contenus).
     */
    suspend fun getUserAnalyticsSummary(
        userId: ObjectId,
        periodDays: Int = 30
    ): UserAnalyticsSummary {
        if (!canAccessAdvancedAnalytics(userId)) {
            throw IllegalAccessException("Advanced analytics are only available for Golden Users")
        }

        val periodEnd = Instant.now()
        val periodStart = periodEnd.minus(periodDays.toLong(), ChronoUnit.DAYS)

        val allAnalytics = analyticsRepository.findByUserIdAndPeriodStartBetween(userId, periodStart, periodEnd)
            .collectList()
            .awaitSingle()

        val totalViews = allAnalytics.sumOf { it.views }
        val totalUniqueVisitors = allAnalytics.sumOf { it.uniqueVisitors }
        val totalLikes = allAnalytics.sumOf { it.likes }
        val totalComments = allAnalytics.sumOf { it.comments }
        val totalShares = allAnalytics.sumOf { it.shares }

        val averageEngagementRate = if (allAnalytics.isNotEmpty()) {
            allAnalytics.map { it.engagementRate }.average()
        } else {
            0.0
        }

        // Top contenu par vues
        val topContent = allAnalytics
            .sortedByDescending { it.views }
            .take(10)
            .map { analytics ->
                // Récupérer le titre du contenu
                val title = when (analytics.contentType) {
                    ContentAnalyticsType.BLOG -> {
                        blogRepository.findById(analytics.contentId).awaitSingleOrNull()?.title ?: "Unknown"
                    }
                    ContentAnalyticsType.ARTICLE -> {
                        articleRepository.findById(analytics.contentId).awaitSingleOrNull()?.title ?: "Unknown"
                    }
                }

                ContentAnalyticsSummary(
                    contentId = analytics.contentId,
                    contentType = analytics.contentType,
                    title = title,
                    views = analytics.views,
                    likes = analytics.likes,
                    engagementRate = analytics.engagementRate
                )
            }

        // Agrégation des sources de trafic
        val trafficSources = allAnalytics
            .flatMap { it.trafficSources.entries }
            .groupBy({ it.key }, { it.value })
            .mapValues { (_, values) -> values.sum() }

        // Agrégation des pays
        val topCountries = allAnalytics
            .flatMap { it.viewsByCountry.entries }
            .groupBy({ it.key }, { it.value })
            .mapValues { (_, values) -> values.sum() }
            .toList()
            .sortedByDescending { it.second }
            .take(10)
            .associate { it.first to it.second }

        return UserAnalyticsSummary(
            userId = userId,
            totalViews = totalViews,
            totalUniqueVisitors = totalUniqueVisitors,
            totalLikes = totalLikes,
            totalComments = totalComments,
            totalShares = totalShares,
            averageEngagementRate = averageEngagementRate,
            topContent = topContent,
            trafficSources = trafficSources,
            topCountries = topCountries,
            periodStart = periodStart,
            periodEnd = periodEnd
        )
    }

    /**
     * Met à jour les analytics pour un contenu (appelé lors d'une vue, like, etc.).
     */
    suspend fun updateContentAnalytics(
        userId: ObjectId,
        contentId: ObjectId,
        contentType: ContentAnalyticsType,
        views: Long = 0,
        likes: Long = 0,
        comments: Long = 0,
        shares: Long = 0,
        favorites: Long = 0
    ) {
        // Seulement pour Golden Users
        if (!canAccessAdvancedAnalytics(userId)) {
            return // Ignorer silencieusement pour les non-Golden
        }

        val periodEnd = Instant.now()
        val periodStart = periodEnd.minus(30, ChronoUnit.DAYS)

        val existing = analyticsRepository.findByUserIdAndContentIdAndContentType(userId, contentId, contentType)
            .awaitSingleOrNull()

        val updated = existing?.copy(
            views = existing.views + views,
            likes = existing.likes + likes,
            comments = existing.comments + comments,
            shares = existing.shares + shares,
            favorites = existing.favorites + favorites,
            engagementRate = calculateEngagementRate(
                existing.views + views,
                existing.likes + likes,
                existing.comments + comments,
                existing.shares + shares
            ),
            lastUpdated = Instant.now()
        ) ?: createDefaultAnalytics(
            userId,
            contentId,
            contentType,
            periodStart,
            periodEnd
        ).copy(
            views = views,
            likes = likes,
            comments = comments,
            shares = shares,
            favorites = favorites,
            engagementRate = calculateEngagementRate(views, likes, comments, shares)
        )

        analyticsRepository.save(updated).awaitSingle()
    }

    private fun calculateEngagementRate(views: Long, likes: Long, comments: Long, shares: Long): Double {
        if (views == 0L) return 0.0
        val totalEngagements = likes + comments + shares
        return (totalEngagements.toDouble() / views.toDouble()) * 100.0
    }

    private suspend fun createDefaultAnalytics(
        userId: ObjectId,
        contentId: ObjectId,
        contentType: ContentAnalyticsType,
        periodStart: Instant,
        periodEnd: Instant
    ): AdvancedAnalytics {
        val analytics = AdvancedAnalytics(
            userId = userId,
            contentId = contentId,
            contentType = contentType,
            periodStart = periodStart,
            periodEnd = periodEnd
        )
        return analyticsRepository.save(analytics).awaitSingle()
    }
}

