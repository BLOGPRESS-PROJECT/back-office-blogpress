package com.kobe.blogpress_api.repository.storage

import com.kobe.blogpress_api.domain.model.storage.StorageQuota
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
interface StorageQuotaRepository : ReactiveMongoRepository<StorageQuota, ObjectId> {
    fun findByUserId(userId: ObjectId): Mono<StorageQuota>
}

