package com.kobe.blogpress_api.dto.search

import com.kobe.blogpress_api.domain.model.article.ArticleType
import java.time.Instant

data class SearchResultDto(
    val results: List<SearchItemDto>,
    val totalResults: Long,
    val page: Int,
    val size: Int,
    val totalPages: Int,
    val query: String,
    val searchTime: Long // en millisecondes
)

data class SearchItemDto(
    val id: String,
    val type: SearchItemType, // BLOG ou ARTICLE
    val title: String,
    val excerpt: String?,
    val slug: String,
    val coverImageUrl: String?,
    val publicUrl: String,
    val authorId: String,
    val createdAt: Instant,
    val viewCount: Long,
    val likeCount: Long,

    // Spécifique aux articles
    val articleType: ArticleType? = null,
    val blogId: String? = null,
    val blogTitle: String? = null,

    // Score de pertinence (pour trier par pertinence)
    val relevanceScore: Double = 0.0
)

enum class SearchItemType {
    BLOG,
    ARTICLE
}