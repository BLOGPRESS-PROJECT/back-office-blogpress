package com.kobe.blogpress_api.dto.article


import com.kobe.blogpress_api.domain.model.article.ArticleType
import java.time.Instant

data class ArticleSummaryDto(
    val id: String,
    val title: String,
    val excerpt: String?,
    val slug: String,
    val coverImageUrl: String?,
    val tags: List<String>,
    val category: String?,
    val authorId: String,
    val blogId: String?,
    val type: ArticleType,
    val isPublished: Boolean,
    val isPrivate: Boolean,
    val publicUrl: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val readTime: Int,
    val stats: ArticleStats
)

data class ArticleStats(
    val viewCount: Long,
    val likeCount: Long,
    val commentCount: Long,
    val shareCount: Long
)