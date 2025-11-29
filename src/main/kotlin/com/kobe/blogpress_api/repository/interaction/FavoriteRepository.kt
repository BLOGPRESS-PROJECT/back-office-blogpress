package com.kobe.blogpress_api.repository.interaction

import com.kobe.blogpress_api.domain.interaction.ContentType
import com.kobe.blogpress_api.domain.interaction.Favorite
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface FavoriteRepository : ReactiveMongoRepository<Favorite, ObjectId> {

    fun existsByContentIdAndUserIdAndContentType(
        contentId: ObjectId,
        userId: ObjectId,
        contentType: ContentType
    ): Mono<Boolean>

    fun findByContentIdAndUserIdAndContentType(
        contentId: ObjectId,
        userId: ObjectId,
        contentType: ContentType
    ): Mono<Favorite>

    fun countByContentIdAndContentType(contentId: ObjectId, contentType: ContentType): Mono<Long>
    
    fun findByUserIdAndContentType(userId: ObjectId, contentType: ContentType): Flux<Favorite>
}