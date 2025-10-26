package com.kobe.blogpress_api.services.interaction

import com.kobe.blogpress_api.domain.interaction.ContentType
import com.kobe.blogpress_api.domain.interaction.Favorite
import com.kobe.blogpress_api.domain.interaction.Like
import com.kobe.blogpress_api.repository.interaction.FavoriteRepository
import com.kobe.blogpress_api.repository.interaction.LikeRepository
import com.kobe.blogpress_api.services.blog.BlogService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.springframework.stereotype.Service

@Service
class ContentInteractionService(
    private val likeRepository: LikeRepository,
    private val favoriteRepository: FavoriteRepository,
    private val blogService: BlogService
    // TODO: Injecter ArticleService quand il sera créé
    // private val articleService: ArticleService
) {

    suspend fun toggleLike(contentId: ObjectId, userId: ObjectId, contentType: ContentType): LikeResponse {
        val existingLike = likeRepository.findByContentIdAndUserIdAndContentType(
            contentId, userId, contentType
        ).awaitSingleOrNull()

        return if (existingLike != null) {
            // Unlike
            likeRepository.delete(existingLike).awaitSingleOrNull()

            // Décrémenter le compteur
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

            // Incrémenter le compteur
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

            // Décrémenter le compteur
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

            // Incrémenter le compteur
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
            ContentType.ARTICLE -> {
                // Sera implémenté quand ArticleService sera créé
                throw UnsupportedOperationException("Article interactions not yet implemented")
            }
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
                // Sera implémenté quand ArticleService sera créé
                throw UnsupportedOperationException("Article interactions not yet implemented")
            }
        }
    }

    // Méthodes privées pour incrémenter/décrémenter les compteurs
    private suspend fun incrementLikeCount(contentId: ObjectId, contentType: ContentType) {
        when (contentType) {
            ContentType.BLOG -> blogService.incrementLikeCount(contentId)
            ContentType.ARTICLE -> {
                // Sera implémenté quand ArticleService sera créé
                // articleService.incrementLikeCount(contentId)
            }
        }
    }

    private suspend fun decrementLikeCount(contentId: ObjectId, contentType: ContentType) {
        when (contentType) {
            ContentType.BLOG -> blogService.decrementLikeCount(contentId)
            ContentType.ARTICLE -> {
                // Sera implémenté quand ArticleService sera créé
                // articleService.decrementLikeCount(contentId)
            }
        }
    }

    private suspend fun incrementFavoriteCount(contentId: ObjectId, contentType: ContentType) {
        when (contentType) {
            ContentType.BLOG -> blogService.incrementFavoriteCount(contentId)
            ContentType.ARTICLE -> {
                // Sera implémenté quand ArticleService sera créé
                // articleService.incrementFavoriteCount(contentId)
            }
        }
    }

    private suspend fun decrementFavoriteCount(contentId: ObjectId, contentType: ContentType) {
        when (contentType) {
            ContentType.BLOG -> blogService.decrementFavoriteCount(contentId)
            ContentType.ARTICLE -> {
                // Sera implémenté quand ArticleService sera créé
                // articleService.decrementFavoriteCount(contentId)
            }
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