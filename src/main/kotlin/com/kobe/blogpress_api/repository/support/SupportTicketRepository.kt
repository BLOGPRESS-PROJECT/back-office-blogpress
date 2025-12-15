package com.kobe.blogpress_api.repository.support

import com.kobe.blogpress_api.domain.model.support.SupportTicket
import com.kobe.blogpress_api.domain.model.support.TicketStatus
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface SupportTicketRepository : ReactiveMongoRepository<SupportTicket, ObjectId> {
    fun findByUserId(userId: ObjectId): Flux<SupportTicket>
    fun findByStatus(status: TicketStatus): Flux<SupportTicket>
    fun findByUserIdAndStatus(userId: ObjectId, status: TicketStatus): Flux<SupportTicket>
    fun findByAssignedTo(adminId: ObjectId): Flux<SupportTicket>
}

