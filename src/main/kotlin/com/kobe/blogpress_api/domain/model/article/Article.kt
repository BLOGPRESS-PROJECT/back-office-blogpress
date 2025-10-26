package com.kobe.blogpress_api.domain.model.article

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "articles")
@CompoundIndexes(
    CompoundIndex(name = "blog_slug_idx", def = "{'blogId': 1, 'slug': 1}", unique = true, sparse = true)
)
data class Article(
    @Id
    val id: ObjectId = ObjectId(),
    val title: String,
    val content: String, // HTML formaté de l'éditeur TipTap
    val excerpt: String? = null,

    @Indexed
    val slug: String,
    val coverImageUrl: String? = null,

    @Indexed
    val tags: List<String> = emptyList(),

    @Indexed
    val category: String? = null,

    @Indexed
    val authorId: ObjectId,

    @Indexed
    val blogId: ObjectId? = null, // null pour SIMPLE_ARTICLE, obligatoire pour BLOG_POST
    val type: ArticleType,
    val isPublished: Boolean = false,
    val isPrivate: Boolean = false,
    val publishAt: Instant? = null,

    @Indexed
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    // Statistics
    val viewCount: Long = 0,
    val likeCount: Long = 0,
    val commentCount: Long = 0,
    val shareCount: Long = 0,
    // Calculé automatiquement
    val readTime: Int = 1 // En minutes
)