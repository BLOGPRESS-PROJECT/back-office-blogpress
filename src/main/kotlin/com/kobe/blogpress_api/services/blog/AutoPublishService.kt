package com.kobe.blogpress_api.services.blog

import com.kobe.blogpress_api.domain.model.article.Article
import com.kobe.blogpress_api.domain.model.blog.Blog
import com.kobe.blogpress_api.repository.blog.BlogRepository
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class AutoPublishService(
    private val blogRepository: BlogRepository,
    private val mongoTemplate: ReactiveMongoTemplate
) {

    private val logger = LoggerFactory.getLogger(AutoPublishService::class.java)

    /**
     * Publie automatiquement les blogs et articles dont la date de publication programmée est arrivée
     * Cette méthode est appelée toutes les minutes
     */
    @Scheduled(fixedRate = 60000) // Toutes les 60 secondes (1 minute)
    fun publishScheduledContent() {
        try {
            val now = Instant.now()
            
            // Publier les blogs programmés
            publishScheduledBlogs(now)
            
            // Publier les articles programmés
            publishScheduledArticles(now)
        } catch (e: Exception) {
            logger.error("❌ Erreur lors de la publication automatique du contenu", e)
        }
    }

    /**
     * Publie automatiquement les blogs dont la date de publication programmée est arrivée
     */
    private fun publishScheduledBlogs(now: Instant) {
        try {
            // Trouver tous les blogs avec publishAt <= maintenant et isPublished = false
            val query = Query(
                Criteria.where("publishAt").lte(now)
                    .and("isPublished").`is`(false)
            )
            
            val update = Update()
                .set("isPublished", true)
                .set("lastPublishedAt", now)
                .set("updatedAt", now)
            
            val result = mongoTemplate.updateMulti(query, update, Blog::class.java).block()
            
            if (result != null && result.modifiedCount > 0) {
                logger.info("✅ ${result.modifiedCount} blog(s) publié(s) automatiquement")
            }
        } catch (e: Exception) {
            logger.error("❌ Erreur lors de la publication automatique des blogs", e)
        }
    }

    /**
     * Publie automatiquement les articles dont la date de publication programmée est arrivée
     */
    private fun publishScheduledArticles(now: Instant) {
        try {
            // Trouver tous les articles avec publishAt <= maintenant et isPublished = false
            val query = Query(
                Criteria.where("publishAt").lte(now)
                    .and("isPublished").`is`(false)
            )
            
            val update = Update().set("isPublished", true).set("updatedAt", now)
            
            val result = mongoTemplate.updateMulti(query, update, Article::class.java).block()
            
            if (result != null && result.modifiedCount > 0) {
                logger.info("✅ ${result.modifiedCount} article(s) publié(s) automatiquement")
            }
        } catch (e: Exception) {
            logger.error("❌ Erreur lors de la publication automatique des articles", e)
        }
    }
}

