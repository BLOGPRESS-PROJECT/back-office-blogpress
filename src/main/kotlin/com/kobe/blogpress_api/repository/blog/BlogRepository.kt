package com.kobe.blogpress_api.repository.blog

import com.kobe.blogpress_api.domain.model.blog.Blog
import org.bson.types.ObjectId
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

@Repository
interface BlogRepository : ReactiveMongoRepository<Blog, ObjectId> {

    fun findBySlug(slug: String): Mono<Blog>
    
    fun findByShareId(shareId: String): Mono<Blog>

    fun existsBySlug(slug: String): Mono<Boolean>

    fun existsBySlugAndIdNot(slug: String, id: ObjectId): Mono<Boolean>

    fun findByAuthorId(authorId: ObjectId): Flux<Blog>

    fun findByAuthorIdAndIsPublished(authorId: ObjectId, isPublished: Boolean, pageable: Pageable): Flux<Blog>

    fun findByIsPublishedAndIsPrivate(isPublished: Boolean, isPrivate: Boolean, pageable: Pageable): Flux<Blog>

    fun countByAuthorId(authorId: ObjectId): Mono<Long>

    fun findByPublishAtBeforeAndIsPublished(publishAt: Instant, isPublished: Boolean): Flux<Blog>
}