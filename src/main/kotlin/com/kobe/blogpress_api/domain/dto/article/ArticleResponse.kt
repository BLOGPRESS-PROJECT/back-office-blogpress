package com.kobe.blogpress_api.domain.dto.article

import com.kobe.blogpress_api.domain.model.article.ArticleType
import java.time.Instant

data class ArticleResponse(
    val id: String,
    val title: String,
    val content: String,
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
    val publishAt: Instant?,
    val publicUrl: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val viewCount: Long,
    val likeCount: Long,
    val commentCount: Long,
    val shareCount: Long,
    val readTime: Int
)