package com.kobe.blogpress_api.configuration

import com.kobe.blogpress_api.domain.model.user.User
import com.mongodb.reactivestreams.client.MongoClient
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.index.IndexOperations
import reactor.core.publisher.Flux

@Configuration
class MongoConfig(
    private val mongoTemplate: ReactiveMongoTemplate
) {

    private val logger = LoggerFactory.getLogger(MongoConfig::class.java)

    @PostConstruct
    fun initIndexes() {
        createUserIndexes()
            .subscribe(
                { indexName ->
                    logger.info("✅ Index created: $indexName")
                },
                { error ->
                    logger.error("❌ Failed to create indexes: ${error.message}")
                },
                {
                    logger.info("✅ All indexes created successfully")
                }
            )
    }

    private fun createUserIndexes(): Flux<String> {
        val indexOps: IndexOperations = mongoTemplate.indexOps(User::class.java) as IndexOperations

        return Flux.merge(
            // Index unique sur email
            indexOps.ensureIndex(
                Index()
                    .on("email", Sort.Direction.ASC)
                    .unique()
                    .named("idx_email_unique")
            ),

            // Index unique sur username
            indexOps.ensureIndex(
                Index()
                    .on("username", Sort.Direction.ASC)
                    .unique()
                    .named("idx_username_unique")
            ),

            // Index sur role
            indexOps.ensureIndex(
                Index()
                    .on("role", Sort.Direction.ASC)
                    .named("idx_role")
            ),

            // Index sur createdAt (pour trier par date)
            indexOps.ensureIndex(
                Index()
                    .on("createdAt", Sort.Direction.DESC)
                    .named("idx_created_at")
            ),

            // Index sur isActive
            indexOps.ensureIndex(
                Index()
                    .on("isActive", Sort.Direction.ASC)
                    .named("idx_is_active")
            ),

            // Index composé pour les followers (recherche rapide)
            indexOps.ensureIndex(
                Index()
                    .on("followers", Sort.Direction.ASC)
                    .named("idx_followers")
            ),

            // Index composé pour les following (recherche rapide)
            indexOps.ensureIndex(
                Index()
                    .on("following", Sort.Direction.ASC)
                    .named("idx_following")
            )
        )
    }
}