package com.kobe.blogpress_api.dto.blog

import jakarta.validation.constraints.Size
import java.time.Instant

data class BlogSummaryDto(
    val id: String,
    val title: String,
    val description: String?,
    val slug: String,
    val logoImageUrl: String?,
    val coverImageUrl: String?,
    val tags: List<String>?,
    val authorId: String,
    val isPublished: Boolean,
    val isPrivate: Boolean,
    val publicUrl: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val postCount: Long,
    val stats: BlogStats
)

data class BlogStats(
    val viewCount: Long,
    val likeCount: Long,
    val shareCount: Long,
    val favoriteCount: Long
)