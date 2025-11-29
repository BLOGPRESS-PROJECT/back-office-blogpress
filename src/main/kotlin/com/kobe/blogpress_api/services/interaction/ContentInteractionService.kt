package com.kobe.blogpress_api.services.interaction

import com.kobe.blogpress_api.domain.interaction.ContentType
import com.kobe.blogpress_api.domain.interaction.Favorite
import com.kobe.blogpress_api.domain.interaction.Like
import com.kobe.blogpress_api.repository.interaction.FavoriteRepository
import com.kobe.blogpress_api.repository.interaction.LikeRepository
import com.kobe.blogpress_api.services.article.ArticleService
import com.kobe.blogpress_api.services.blog.BlogService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.springframework.stereotype.Service

@Service
class ContentInteractionService(
    private val likeRepository: LikeRepository,
    private val favoriteRepository: FavoriteRepository,
    private val blogService: BlogService,
    private val articleService: ArticleService
) {

    suspend fun toggleLike(contentId: ObjectId, userId: ObjectId, contentType: ContentType): LikeResponse {
        val existingLike = likeRepository.findByContentIdAndUserIdAndContentType(
            contentId, userId, contentType
        ).awaitSingleOrNull()

        return if (existingLike != null) {
            // Unlike
            likeRepository.delete(existingLike).awaitSingleOrNull()
            decrementLikeCount(contentId, contentType)

            val count = likeRepository.countByContentIdAndContentType(contentId, contentType).awaitSingle()
            LikeResponse(
                contentId = contentId.toHexString(),
                isLiked = false,
                likeCount = count
            )
        } else {
            // Like
            val like = Like(
                contentId = contentId,
                contentType = contentType,
                userId = userId
            )
            likeRepository.save(like).awaitSingle()
            incrementLikeCount(contentId, contentType)

            val count = likeRepository.countByContentIdAndContentType(contentId, contentType).awaitSingle()
            LikeResponse(
                contentId = contentId.toHexString(),
                isLiked = true,
                likeCount = count
            )
        }
    }

    suspend fun toggleFavorite(contentId: ObjectId, userId: ObjectId, contentType: ContentType): FavoriteResponse {
        val existingFavorite = favoriteRepository.findByContentIdAndUserIdAndContentType(
            contentId, userId, contentType
        ).awaitSingleOrNull()

        return if (existingFavorite != null) {
            // Unfavorite
            favoriteRepository.delete(existingFavorite).awaitSingleOrNull()
            decrementFavoriteCount(contentId, contentType)

            FavoriteResponse(
                contentId = contentId.toHexString(),
                isFavorited = false
            )
        } else {
            // Favorite
            val favorite = Favorite(
                contentId = contentId,
                contentType = contentType,
                userId = userId
            )
            favoriteRepository.save(favorite).awaitSingle()
            incrementFavoriteCount(contentId, contentType)

            FavoriteResponse(
                contentId = contentId.toHexString(),
                isFavorited = true
            )
        }
    }

    suspend fun incrementView(contentId: ObjectId, contentType: ContentType) {
        when (contentType) {
            ContentType.BLOG -> blogService.incrementViewCount(contentId)
            ContentType.ARTICLE -> articleService.incrementViewCount(contentId)
        }
    }

    suspend fun incrementShare(contentId: ObjectId, contentType: ContentType): ShareResponse {
        when (contentType) {
            ContentType.BLOG -> {
                blogService.incrementShareCount(contentId)
                val blog = blogService.getBlogById(contentId)
                return ShareResponse(
                    contentId = contentId.toHexString(),
                    shareCount = blog.shareCount,
                    shareUrl = blog.publicUrl
                )
            }
            ContentType.ARTICLE -> {
                articleService.incrementShareCount(contentId)
                val article = articleService.getArticleById(contentId)
                return ShareResponse(
                    contentId = contentId.toHexString(),
                    shareCount = article.shareCount,
                    shareUrl = article.publicUrl
                )
            }
        }
    }

    private suspend fun incrementLikeCount(contentId: ObjectId, contentType: ContentType) {
        when (contentType) {
            ContentType.BLOG -> blogService.incrementLikeCount(contentId)
            ContentType.ARTICLE -> articleService.incrementLikeCount(contentId)
        }
    }

    private suspend fun decrementLikeCount(contentId: ObjectId, contentType: ContentType) {
        when (contentType) {
            ContentType.BLOG -> blogService.decrementLikeCount(contentId)
            ContentType.ARTICLE -> articleService.decrementLikeCount(contentId)
        }
    }

    private suspend fun incrementFavoriteCount(contentId: ObjectId, contentType: ContentType) {
        when (contentType) {
            ContentType.BLOG -> blogService.incrementFavoriteCount(contentId)
            ContentType.ARTICLE -> articleService.incrementFavoriteCount(contentId)
        }
    }

    private suspend fun decrementFavoriteCount(contentId: ObjectId, contentType: ContentType) {
        when (contentType) {
            ContentType.BLOG -> blogService.decrementFavoriteCount(contentId)
            ContentType.ARTICLE -> articleService.decrementFavoriteCount(contentId)
        }
    }
}

data class LikeResponse(
    val contentId: String,
    val isLiked: Boolean,
    val likeCount: Long
)

data class FavoriteResponse(
    val contentId: String,
    val isFavorited: Boolean
)

data class ShareResponse(
    val contentId: String,
    val shareCount: Long,
    val shareUrl: String? = null
)

interface ArticleService {
    suspend fun incrementViewCount(articleId: ObjectId)
    suspend fun incrementLikeCount(articleId: ObjectId)
    suspend fun decrementLikeCount(articleId: ObjectId)
    suspend fun incrementShareCount(articleId: ObjectId)
    suspend fun getArticleShareInfo(articleId: ObjectId): ArticleShareInfo
}

data class ArticleShareInfo(
    val shareCount: Long,
    val publicUrl: String?
)