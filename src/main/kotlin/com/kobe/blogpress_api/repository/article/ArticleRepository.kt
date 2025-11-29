package com.kobe.blogpress_api.repository.article

import com.kobe.blogpress_api.domain.model.article.Article
import com.kobe.blogpress_api.domain.model.article.ArticleType
import org.bson.types.ObjectId
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

@Repository
interface ArticleRepository : ReactiveMongoRepository<Article, ObjectId> {

    // Recherche par slug
    fun findBySlug(slug: String): Mono<Article>

    fun findByBlogIdAndSlug(blogId: ObjectId, slug: String): Mono<Article>

    // Vérification d'existence
    fun existsBySlug(slug: String): Mono<Boolean>

    fun existsBySlugAndIdNot(slug: String, id: ObjectId): Mono<Boolean>

    fun existsByBlogIdAndSlug(blogId: ObjectId, slug: String): Mono<Boolean>

    fun existsByBlogIdAndSlugAndIdNot(blogId: ObjectId, slug: String, id: ObjectId): Mono<Boolean>

    // Par auteur
    fun findByAuthorId(authorId: ObjectId): Flux<Article>

    fun findByAuthorIdAndType(authorId: ObjectId, type: ArticleType, pageable: Pageable): Flux<Article>

    fun findByAuthorIdAndIsPublished(authorId: ObjectId, isPublished: Boolean, pageable: Pageable): Flux<Article>

    // Par blog
    fun findByBlogId(blogId: ObjectId, pageable: Pageable): Flux<Article>

    fun findByBlogIdAndIsPublishedAndIsPrivate(
        blogId: ObjectId,
        isPublished: Boolean,
        isPrivate: Boolean,
        pageable: Pageable
    ): Flux<Article>

    fun countByBlogId(blogId: ObjectId): Mono<Long>

    // Articles publics
    fun findByIsPublishedAndIsPrivateAndType(
        isPublished: Boolean,
        isPrivate: Boolean,
        type: ArticleType,
        pageable: Pageable
    ): Flux<Article>

    fun findByIsPublishedAndIsPrivate(
        isPublished: Boolean,
        isPrivate: Boolean,
        pageable: Pageable
    ): Flux<Article>

    // Par catégorie
    fun findByCategoryAndIsPublishedAndIsPrivate(
        category: String,
        isPublished: Boolean,
        isPrivate: Boolean,
        pageable: Pageable
    ): Flux<Article>

    // Par tags
    fun findByTagsContainingAndIsPublishedAndIsPrivate(
        tag: String,
        isPublished: Boolean,
        isPrivate: Boolean,
        pageable: Pageable
    ): Flux<Article>

    // Publication programmée
    fun findByPublishAtBeforeAndIsPublished(publishAt: Instant, isPublished: Boolean): Flux<Article>

    // Compteurs
    fun countByAuthorId(authorId: ObjectId): Mono<Long>

    fun countByAuthorIdAndType(authorId: ObjectId, type: ArticleType): Mono<Long>
    
    // ⭐ NOUVEAU : Recherche par shareId
    fun findByShareId(shareId: java.util.UUID): Mono<Article>
    
    // ⭐ NOUVEAU : Recherche par shareId et blogId (pour les BLOG_POST)
    fun findByShareIdAndBlogId(shareId: java.util.UUID, blogId: ObjectId): Mono<Article>
    
    // ⭐ NOUVEAU : Supprimer tous les articles d'un blog
    fun deleteByBlogId(blogId: ObjectId): Mono<Void>
    
    // ⭐ NOUVEAU : Récupérer tous les articles d'un blog (sans pagination)
    fun findAllByBlogId(blogId: ObjectId): Flux<Article>
}