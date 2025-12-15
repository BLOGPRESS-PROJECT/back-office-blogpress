package com.kobe.blogpress_api.services.support

import com.kobe.blogpress_api.domain.model.support.SupportCategory
import com.kobe.blogpress_api.domain.model.support.SupportTicket
import com.kobe.blogpress_api.domain.model.support.TicketMessage
import com.kobe.blogpress_api.domain.model.support.TicketPriority
import com.kobe.blogpress_api.domain.model.support.TicketStatus
import com.kobe.blogpress_api.repository.support.SupportTicketRepository
import com.kobe.blogpress_api.services.user.UserService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Service pour gérer les tickets de support.
 * Les Golden Users ont une priorité automatique HIGH.
 */
@Service
class SupportService(
    private val supportTicketRepository: SupportTicketRepository,
    private val userService: UserService
) {
    private val logger = LoggerFactory.getLogger(SupportService::class.java)

    /**
     * Crée un nouveau ticket de support.
     * La priorité est automatiquement définie selon le statut Golden de l'utilisateur.
     */
    suspend fun createTicket(
        userId: ObjectId,
        subject: String,
        description: String,
        category: SupportCategory,
        priority: TicketPriority? = null,
        attachments: List<String> = emptyList()
    ): SupportTicket {
        val user = userService.findById(userId)

        // Priorité automatique : HIGH pour Golden Users, LOW pour les autres (sauf si spécifiée)
        val finalPriority = priority ?: if (user.isGoldenUser) {
            TicketPriority.HIGH
        } else {
            TicketPriority.LOW
        }

        val ticket = SupportTicket(
            userId = userId,
            subject = subject,
            description = description,
            category = category,
            priority = finalPriority,
            isGoldenUser = user.isGoldenUser,
            attachments = attachments,
            status = TicketStatus.OPEN
        )

        logger.info("Support ticket created: ${ticket.id.toHexString()} by user: ${userId.toHexString()} (Golden: ${user.isGoldenUser})")
        return supportTicketRepository.save(ticket).awaitSingle()
    }

    /**
     * Récupère tous les tickets d'un utilisateur.
     */
    suspend fun getUserTickets(userId: ObjectId): List<SupportTicket> {
        return supportTicketRepository.findByUserId(userId)
            .collectList()
            .awaitSingle()
    }

    /**
     * Récupère un ticket spécifique (vérifie que l'utilisateur est le propriétaire ou un admin).
     */
    suspend fun getTicket(ticketId: ObjectId, userId: ObjectId, isAdmin: Boolean = false): SupportTicket {
        val ticket = supportTicketRepository.findById(ticketId).awaitSingleOrNull()
            ?: throw IllegalArgumentException("Ticket not found")

        if (!isAdmin && ticket.userId != userId) {
            throw IllegalAccessException("You are not authorized to view this ticket")
        }

        return ticket
    }

    /**
     * Ajoute un message à un ticket.
     */
    suspend fun addMessage(
        ticketId: ObjectId,
        authorId: ObjectId,
        authorRole: String,
        content: String,
        attachments: List<String> = emptyList(),
        isInternal: Boolean = false
    ): SupportTicket {
        val ticket = getTicket(ticketId, authorId, authorRole == "ADMIN")

        val message = TicketMessage(
            authorId = authorId,
            authorRole = authorRole,
            content = content,
            attachments = attachments,
            isInternal = isInternal
        )

        val updatedTicket = ticket.copy(
            messages = ticket.messages + message,
            status = if (authorRole == "ADMIN" || authorRole == "SUPPORT") {
                TicketStatus.IN_PROGRESS
            } else {
                TicketStatus.WAITING_USER
            },
            firstResponseAt = ticket.firstResponseAt ?: if (authorRole == "ADMIN" || authorRole == "SUPPORT") {
                Instant.now()
            } else {
                null
            },
            updatedAt = Instant.now()
        )

        logger.info("Message added to ticket: ${ticketId.toHexString()} by $authorRole")
        return supportTicketRepository.save(updatedTicket).awaitSingle()
    }

    /**
     * Récupère tous les tickets ouverts (pour les admins).
     */
    suspend fun getOpenTickets(): List<SupportTicket> {
        return supportTicketRepository.findByStatus(TicketStatus.OPEN)
            .collectList()
            .awaitSingle()
    }

    /**
     * Récupère les tickets assignés à un admin.
     */
    suspend fun getAssignedTickets(adminId: ObjectId): List<SupportTicket> {
        return supportTicketRepository.findByAssignedTo(adminId)
            .collectList()
            .awaitSingle()
    }

    /**
     * Assigne un ticket à un admin.
     */
    suspend fun assignTicket(ticketId: ObjectId, adminId: ObjectId): SupportTicket {
        val ticket = supportTicketRepository.findById(ticketId).awaitSingleOrNull()
            ?: throw IllegalArgumentException("Ticket not found")

        val updated = ticket.copy(
            assignedTo = adminId,
            status = TicketStatus.IN_PROGRESS,
            updatedAt = Instant.now()
        )

        logger.info("Ticket ${ticketId.toHexString()} assigned to admin: ${adminId.toHexString()}")
        return supportTicketRepository.save(updated).awaitSingle()
    }

    /**
     * Résout un ticket.
     */
    suspend fun resolveTicket(ticketId: ObjectId, adminId: ObjectId): SupportTicket {
        val ticket = getTicket(ticketId, adminId, true)

        val updated = ticket.copy(
            status = TicketStatus.RESOLVED,
            resolvedAt = Instant.now(),
            updatedAt = Instant.now()
        )

        logger.info("Ticket ${ticketId.toHexString()} resolved by admin: ${adminId.toHexString()}")
        return supportTicketRepository.save(updated).awaitSingle()
    }

    /**
     * Ferme un ticket.
     */
    suspend fun closeTicket(ticketId: ObjectId, userId: ObjectId): SupportTicket {
        val ticket = getTicket(ticketId, userId)

        val updated = ticket.copy(
            status = TicketStatus.CLOSED,
            updatedAt = Instant.now()
        )

        logger.info("Ticket ${ticketId.toHexString()} closed by user: ${userId.toHexString()}")
        return supportTicketRepository.save(updated).awaitSingle()
    }
}

