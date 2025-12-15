package com.kobe.blogpress_api.controller.golden

import com.kobe.blogpress_api.dto.common.ApiResponseDto
import com.kobe.blogpress_api.dto.support.StorageQuotaDTO
import com.kobe.blogpress_api.services.storage.StorageQuotaService
import kotlinx.coroutines.reactor.mono
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.*

@RestController
@RequestMapping("/api/storage")
class StorageQuotaController(
    private val storageQuotaService: StorageQuotaService
) {
    private val logger = LoggerFactory.getLogger(StorageQuotaController::class.java)

    /**
     * Récupère le quota de stockage de l'utilisateur connecté.
     * GET /api/storage/quota
     */
    @GetMapping("/quota")
    fun getMyStorageQuota(
        @AuthenticationPrincipal userId: String
    ): Mono<ResponseEntity<ApiResponseDto<StorageQuotaDTO>>> = mono {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get storage quota for user: $userId")

        try {
            val quota = storageQuotaService.getQuota(ObjectId(userId))

            val quotaDTO = StorageQuotaDTO(
                userId = quota.userId.toHexString(),
                usedStorageBytes = quota.usedStorageBytes,
                maxStorageBytes = quota.maxStorageBytes,
                isUnlimited = quota.isUnlimited,
                usagePercentage = quota.getUsagePercentage(),
                remainingBytes = quota.getRemainingBytes(),
                usedStorageMB = quota.usedStorageBytes / (1024.0 * 1024.0),
                maxStorageMB = if (quota.isUnlimited) Double.MAX_VALUE else quota.maxStorageBytes / (1024.0 * 1024.0),
                remainingStorageMB = if (quota.isUnlimited) Double.MAX_VALUE else quota.getRemainingBytes() / (1024.0 * 1024.0)
            )

            ResponseEntity.ok(
                ApiResponseDto.success(
                    data = quotaDTO,
                    message = "Storage quota retrieved successfully",
                    requestId = requestId
                )
            )
        } catch (e: Exception) {
            logger.error("[$requestId] Error retrieving storage quota for user: $userId", e)
            ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error(
                    message = "Error retrieving storage quota",
                    requestId = requestId
                ))
        }
    }
}

