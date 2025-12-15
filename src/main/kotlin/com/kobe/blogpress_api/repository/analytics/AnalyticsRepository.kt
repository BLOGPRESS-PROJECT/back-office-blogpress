package com.kobe.blogpress_api.repository.analytics

import com.kobe.blogpress_api.domain.model.analytics.AdvancedAnalytics
import com.kobe.blogpress_api.domain.model.analytics.ContentAnalyticsType
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

@Repository
interface AnalyticsRepository : ReactiveMongoRepository<AdvancedAnalytics, ObjectId> {
    fun findByUserIdAndContentType(
        userId: ObjectId,
        contentType: ContentAnalyticsType
    ): Flux<AdvancedAnalytics>

    fun findByUserIdAndPeriodStartBetween(
        userId: ObjectId,
        start: Instant,
        end: Instant
    ): Flux<AdvancedAnalytics>

    fun findByUserIdAndContentIdAndContentType(
        userId: ObjectId,
        contentId: ObjectId,
        contentType: ContentAnalyticsType
    ): Mono<AdvancedAnalytics>
}

