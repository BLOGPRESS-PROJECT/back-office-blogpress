package com.kobe.blogpress_api.repository.audit

import com.kobe.blogpress_api.model.audit.AuditLog
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import java.time.Instant

interface AuditLogRepository : MongoRepository<AuditLog, ObjectId> {
    fun findByUserIdAndTimestampBetween(
        userId: ObjectId,
        startDate: Instant,
        endDate: Instant
    ): List<AuditLog>

    fun findByUserIdAndTimestampAfter(
        userId: ObjectId,
        date: Instant
    ): List<AuditLog>

    fun findByActionAndTimestampBetween(
        action: String,
        startDate: Instant,
        endDate: Instant
    ): List<AuditLog>
}