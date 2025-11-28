package com.kobe.blogpress_api.dto.blog

import jakarta.validation.constraints.Size

data class BlogSummaryDto(
    val id: String,
    val title: String,
    val description: String?,
    val slug: String,
    val shareId: String, // Identifiant unique pour le partage (UUID)
    val logoImageUrl: String?,
    val coverImageUrl: String?,
    val tags: List<String>?,
    val authorId: String,
    val isPublished: Boolean,
    val isPrivate: Boolean,
    val publicUrl: String,
    val createdAt: String, // ISO 8601 format
    val updatedAt: String, // ISO 8601 format
    val postCount: Long,
    val stats: BlogStats
)

data class BlogStats(
    val viewCount: Long,
    val likeCount: Long,
    val shareCount: Long,
    val favoriteCount: Long
)

data class BlogGlobalStatsResponse(
    val totalBlogs: Long,
    val totalViews: Long,
    val totalLikes: Long,
    val totalShares: Long,
    val totalFavorites: Long
)