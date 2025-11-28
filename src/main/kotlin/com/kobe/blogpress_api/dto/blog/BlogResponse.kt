package com.kobe.blogpress_api.dto.blog

import java.time.Instant

data class BlogResponse(
    val id: String,
    val title: String,
    val description: String?,
    val slug: String,
    val logoImageUrl: String?,
    val coverImageUrl: String?,
    val tags: List<String>,
    val authorId: String,
    val isPublished: Boolean,
    val isPrivate: Boolean,
    val publishAt: Instant?,
    val publicUrl: String, // URL publique complète du blog (non-nullable)
    val createdAt: Instant,
    val updatedAt: Instant,
    val postCount: Long,
    val viewCount: Long,
    val likeCount: Long,
    val shareCount: Long,
    val favoriteCount: Long
)