package com.kobe.blogpress_api.controller.golden

import com.kobe.blogpress_api.dto.common.ApiResponseDto
import com.kobe.blogpress_api.dto.support.AddMessageRequestDTO
import com.kobe.blogpress_api.dto.support.CreateTicketRequestDTO
import com.kobe.blogpress_api.dto.support.SupportTicketDTO
import com.kobe.blogpress_api.dto.support.TicketMessageDTO
import com.kobe.blogpress_api.services.support.SupportService
import jakarta.validation.Valid
import kotlinx.coroutines.reactor.mono
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.util.*

/**
 * Contrôleur pour les tickets de support.
 * Les Golden Users ont une priorité automatique HIGH.
 */
@RestController
@RequestMapping("/api/support")
class SupportController(
    private val supportService: SupportService
) {
    private val logger = LoggerFactory.getLogger(SupportController::class.java)

    /**
     * Crée un nouveau ticket de support.
     * POST /api/support/tickets
     */
    @PostMapping("/tickets")
    fun createTicket(
        @AuthenticationPrincipal userId: String,
        @Valid @RequestBody request: CreateTicketRequestDTO
    ): Mono<ResponseEntity<ApiResponseDto<SupportTicketDTO>>> = mono {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Create support ticket by user: $userId")

        try {
            val ticket = supportService.createTicket(
                ObjectId(userId),
                request.subject,
                request.description,
                request.category,
                request.priority,
                request.attachments
            )

            val ticketDTO = toTicketDTO(ticket)

            ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(
                    data = ticketDTO,
                    message = "Support ticket created successfully",
                    requestId = requestId
                ))
        } catch (e: Exception) {
            logger.error("[$requestId] Error creating support ticket", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error(
                    message = "Error creating support ticket: ${e.message}",
                    requestId = requestId
                ))
        }
    }

    /**
     * Récupère tous les tickets de l'utilisateur connecté.
     * GET /api/support/tickets
     */
    @GetMapping("/tickets")
    fun getMyTickets(
        @AuthenticationPrincipal userId: String
    ): Mono<ResponseEntity<ApiResponseDto<List<SupportTicketDTO>>>> = mono {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get support tickets for user: $userId")

        try {
            val tickets = supportService.getUserTickets(ObjectId(userId))
            val ticketsDTO = tickets.map { toTicketDTO(it) }

            ResponseEntity.ok(
                ApiResponseDto.success(
                    data = ticketsDTO,
                    message = "Support tickets retrieved successfully",
                    requestId = requestId
                )
            )
        } catch (e: Exception) {
            logger.error("[$requestId] Error retrieving support tickets", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error(
                    message = "Error retrieving support tickets",
                    requestId = requestId
                ))
        }
    }

    /**
     * Récupère un ticket spécifique.
     * GET /api/support/tickets/{ticketId}
     */
    @GetMapping("/tickets/{ticketId}")
    fun getTicket(
        @AuthenticationPrincipal userId: String,
        @PathVariable ticketId: String
    ): Mono<ResponseEntity<ApiResponseDto<SupportTicketDTO>>> = mono {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Get support ticket: $ticketId by user: $userId")

        try {
            val ticket = supportService.getTicket(ObjectId(ticketId), ObjectId(userId))
            val ticketDTO = toTicketDTO(ticket)

            ResponseEntity.ok(
                ApiResponseDto.success(
                    data = ticketDTO,
                    message = "Support ticket retrieved successfully",
                    requestId = requestId
                )
            )
        } catch (e: IllegalAccessException) {
            logger.warn("[$requestId] Access denied for ticket: $ticketId")
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponseDto.error(
                    message = "You are not authorized to view this ticket",
                    requestId = requestId
                ))
        } catch (e: IllegalArgumentException) {
            logger.warn("[$requestId] Ticket not found: $ticketId")
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.error(
                    message = "Ticket not found",
                    requestId = requestId
                ))
        } catch (e: Exception) {
            logger.error("[$requestId] Error retrieving support ticket", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error(
                    message = "Error retrieving support ticket",
                    requestId = requestId
                ))
        }
    }

    /**
     * Ajoute un message à un ticket.
     * POST /api/support/tickets/{ticketId}/messages
     */
    @PostMapping("/tickets/{ticketId}/messages")
    fun addMessage(
        @AuthenticationPrincipal userId: String,
        @PathVariable ticketId: String,
        @Valid @RequestBody request: AddMessageRequestDTO
    ): Mono<ResponseEntity<ApiResponseDto<SupportTicketDTO>>> = mono {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Add message to ticket: $ticketId by user: $userId")

        try {
            // TODO: Récupérer le rôle de l'utilisateur depuis le token ou la base
            val userRole = "USER" // Pour l'instant, à récupérer depuis UserService

            val ticket = supportService.addMessage(
                ObjectId(ticketId),
                ObjectId(userId),
                userRole,
                request.content,
                request.attachments,
                request.isInternal
            )

            val ticketDTO = toTicketDTO(ticket)

            ResponseEntity.ok(
                ApiResponseDto.success(
                    data = ticketDTO,
                    message = "Message added successfully",
                    requestId = requestId
                )
            )
        } catch (e: Exception) {
            logger.error("[$requestId] Error adding message to ticket", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error(
                    message = "Error adding message: ${e.message}",
                    requestId = requestId
                ))
        }
    }

    /**
     * Ferme un ticket.
     * PUT /api/support/tickets/{ticketId}/close
     */
    @PutMapping("/tickets/{ticketId}/close")
    fun closeTicket(
        @AuthenticationPrincipal userId: String,
        @PathVariable ticketId: String
    ): Mono<ResponseEntity<ApiResponseDto<SupportTicketDTO>>> = mono {
        val requestId = UUID.randomUUID().toString()
        logger.info("[$requestId] Close ticket: $ticketId by user: $userId")

        try {
            val ticket = supportService.closeTicket(ObjectId(ticketId), ObjectId(userId))
            val ticketDTO = toTicketDTO(ticket)

            ResponseEntity.ok(
                ApiResponseDto.success(
                    data = ticketDTO,
                    message = "Ticket closed successfully",
                    requestId = requestId
                )
            )
        } catch (e: Exception) {
            logger.error("[$requestId] Error closing ticket", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.error(
                    message = "Error closing ticket: ${e.message}",
                    requestId = requestId
                ))
        }
    }

    private fun toTicketDTO(ticket: com.kobe.blogpress_api.domain.model.support.SupportTicket): SupportTicketDTO {
        return SupportTicketDTO(
            id = ticket.id.toHexString(),
            userId = ticket.userId.toHexString(),
            subject = ticket.subject,
            description = ticket.description,
            category = ticket.category,
            priority = ticket.priority,
            status = ticket.status,
            assignedTo = ticket.assignedTo?.toHexString(),
            isGoldenUser = ticket.isGoldenUser,
            messages = ticket.messages.map { message ->
                TicketMessageDTO(
                    id = message.id.toHexString(),
                    authorId = message.authorId.toHexString(),
                    authorRole = message.authorRole,
                    content = message.content,
                    attachments = message.attachments,
                    createdAt = message.createdAt,
                    isInternal = message.isInternal
                )
            },
            attachments = ticket.attachments,
            tags = ticket.tags,
            createdAt = ticket.createdAt,
            updatedAt = ticket.updatedAt,
            resolvedAt = ticket.resolvedAt,
            firstResponseAt = ticket.firstResponseAt
        )
    }
}

