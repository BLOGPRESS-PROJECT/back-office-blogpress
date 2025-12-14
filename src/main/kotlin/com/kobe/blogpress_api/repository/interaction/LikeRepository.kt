package com.kobe.blogpress_api.repository.interaction

import com.kobe.blogpress_api.domain.interaction.ContentType
import com.kobe.blogpress_api.domain.interaction.Like
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
interface LikeRepository : ReactiveMongoRepository<Like, ObjectId> {

    fun existsByContentIdAndUserIdAndContentType(
        contentId: ObjectId,
        userId: ObjectId,
        contentType: ContentType
    ): Mono<Boolean>

    fun findByContentIdAndUserIdAndContentType(
        contentId: ObjectId,
        userId: ObjectId,
        contentType: ContentType
    ): Mono<Like>

    fun countByContentIdAndContentType(contentId: ObjectId, contentType: ContentType): Mono<Long>
    
    fun deleteByContentIdAndContentType(contentId: ObjectId, contentType: ContentType): Mono<Void>
}