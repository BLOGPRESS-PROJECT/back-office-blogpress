package com.kobe.blogpress_api.controller.interaction

import com.kobe.blogpress_api.domain.interaction.ContentType
import com.kobe.blogpress_api.dto.common.ApiResponseDto
import com.kobe.blogpress_api.services.interaction.ContentInteractionService
import com.kobe.blogpress_api.services.interaction.FavoriteResponse
import com.kobe.blogpress_api.services.interaction.LikeResponse
import com.kobe.blogpress_api.services.interaction.ShareResponse
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/interactions")
class ContentInteractionController(
    private val contentInteractionService: ContentInteractionService
) {

    private val logger = LoggerFactory.getLogger(ContentInteractionController::class.java)

    @PostMapping("/like/toggle")
    suspend fun toggleLike(
        @AuthenticationPrincipal userId: String,
        @PathVariable contentId: String,
        @RequestBody request: ContentTypeRequest
    ): ResponseEntity<ApiResponseDto<LikeResponse>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Toggle like for content: $contentId by user: $userId")

        val response = contentInteractionService.toggleLike(
            ObjectId(contentId),
            ObjectId(userId),
            request.contentType
        )

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = response,
                message = if (response.isLiked) "Content liked" else "Content unliked",
                requestId = requestId
            )
        )
    }

    @PostMapping("/favorite/toggle")
    suspend fun toggleFavorite(
        @AuthenticationPrincipal userId: String,
        @PathVariable contentId: String,
        @RequestBody request: ContentTypeRequest
    ): ResponseEntity<ApiResponseDto<FavoriteResponse>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Toggle favorite for content: $contentId by user: $userId")

        val response = contentInteractionService.toggleFavorite(
            ObjectId(contentId),
            ObjectId(userId),
            request.contentType
        )

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = response,
                message = if (response.isFavorited) "Content added to favorites" else "Content removed from favorites",
                requestId = requestId
            )
        )
    }

    @PostMapping("/view")
    suspend fun incrementView(
        @PathVariable contentId: String,
        @RequestBody request: ContentTypeRequest
    ): ResponseEntity<ApiResponseDto<Nothing>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Increment view for content: $contentId")

        contentInteractionService.incrementView(ObjectId(contentId), request.contentType)

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = null,
                message = "View count incremented",
                requestId = requestId
            )
        ) as ResponseEntity<ApiResponseDto<Nothing>>
    }

    @PostMapping("/share")
    suspend fun incrementShare(
        @PathVariable contentId: String,
        @RequestBody request: ContentTypeRequest
    ): ResponseEntity<ApiResponseDto<ShareResponse>> {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Increment share for content: $contentId")

        val response = contentInteractionService.incrementShare(ObjectId(contentId), request.contentType)

        return ResponseEntity.ok(
            ApiResponseDto.success(
                data = response,
                message = "Content shared",
                requestId = requestId
            )
        )
    }
}

data class ContentTypeRequest(
    val contentType: ContentType
)