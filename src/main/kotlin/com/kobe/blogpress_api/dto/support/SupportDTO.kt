package com.kobe.blogpress_api.dto.support

import com.kobe.blogpress_api.domain.model.support.SupportCategory
import com.kobe.blogpress_api.domain.model.support.TicketPriority
import com.kobe.blogpress_api.domain.model.support.TicketStatus
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class CreateTicketRequestDTO(
    @field:NotBlank(message = "Subject is required")
    val subject: String,

    @field:NotBlank(message = "Description is required")
    val description: String,

    val category: SupportCategory,
    val priority: TicketPriority? = null, // Optionnel, sera déterminé automatiquement selon Golden status
    val attachments: List<String> = emptyList()
)

data class AddMessageRequestDTO(
    @field:NotBlank(message = "Content is required")
    val content: String,

    val attachments: List<String> = emptyList(),
    val isInternal: Boolean = false // Pour les admins uniquement
)

data class TicketMessageDTO(
    val id: String,
    val authorId: String,
    val authorRole: String,
    val content: String,
    val attachments: List<String>,
    val createdAt: Instant,
    val isInternal: Boolean
)

data class SupportTicketDTO(
    val id: String,
    val userId: String,
    val subject: String,
    val description: String,
    val category: SupportCategory,
    val priority: TicketPriority,
    val status: TicketStatus,
    val assignedTo: String?,
    val isGoldenUser: Boolean,
    val messages: List<TicketMessageDTO>,
    val attachments: List<String>,
    val tags: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val resolvedAt: Instant?,
    val firstResponseAt: Instant?
)

data class StorageQuotaDTO(
    val userId: String,
    val usedStorageBytes: Long,
    val maxStorageBytes: Long,
    val isUnlimited: Boolean,
    val usagePercentage: Double,
    val remainingBytes: Long,
    val usedStorageMB: Double,
    val maxStorageMB: Double,
    val remainingStorageMB: Double
)

