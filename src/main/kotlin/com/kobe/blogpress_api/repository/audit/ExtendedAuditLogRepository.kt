package com.kobe.blogpress_api.repository.audit

import com.kobe.blogpress_api.model.audit.AuditLog
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import java.time.Instant

interface ExtendedAuditLogRepository : MongoRepository<AuditLog, ObjectId> {
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

    fun findByActionInAndTimestampBetween(
        actions: List<String>,
        startDate: Instant,
        endDate: Instant
    ): List<AuditLog>

    fun findByResourceAndTimestampBetween(
        resource: String,
        startDate: Instant,
        endDate: Instant
    ): List<AuditLog>

    fun findByIpAddressAndTimestampBetween(
        ipAddress: String,
        startDate: Instant,
        endDate: Instant
    ): List<AuditLog>

    fun findBySuccessAndTimestampBetween(
        success: Boolean,
        startDate: Instant,
        endDate: Instant
    ): List<AuditLog>

    fun countByUserIdAndTimestampBetween(
        userId: ObjectId,
        startDate: Instant,
        endDate: Instant
    ): Long

    @Query("{ 'securityContext.riskLevel': { \$in: ['HIGH', 'MEDIUM'] }, 'timestamp': { \$gte: ?0, \$lte: ?1 } }")

    fun findByTimestampBetween(startDate: Instant, endDate: Instant): List<AuditLog>
}