package com.kobe.blogpress_api.domain.model.blog

import org.bson.types.ObjectId
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "blogs")
@CompoundIndexes(
    CompoundIndex(name = "text_search_idx", def = "{'title': 'text', 'description': 'text'}")
)
data class Blog(
    @Id
    val id: ObjectId = ObjectId(),
    val title: String,
    val description: String? = null,

    @Indexed
    val tags: List<String> = emptyList(),

    @Indexed(unique = true)
    val slug: String,
    val logoImageUrl: String? = null,
    val coverImageUrl: String? = null,

    @Indexed
    val authorId: ObjectId,
    val isPublished: Boolean = false,
    val isPrivate: Boolean = false,
    val publishAt: Instant? = null,
    val lastPublishedAt: Instant? = null, // Date de dernière publication

    // URLs
    val publicUrl: String, // URL publique complète du blog (stockée en base)
    val canonicalUrl: String? = null, // URL canonique pour le SEO (optionnel)

    @Indexed
    @CreatedDate
    val createdAt: Instant = Instant.now(),
    @LastModifiedDate
    val updatedAt: Instant = Instant.now(),

    // Statistics
    val postCount: Long = 0,
    val viewCount: Long = 0,
    val likeCount: Long = 0,
    val shareCount: Long = 0,
    val favoriteCount: Long = 0
)