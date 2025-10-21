package com.kobe.blogpress_api.repository.user

import com.kobe.blogpress_api.model.user.UserSession
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface UserSessionRepository : MongoRepository<UserSession, ObjectId> {
    fun findBySessionId(sessionId: String): UserSession?
    fun findByUserIdAndIsActive(userId: ObjectId, isActive: Boolean): List<UserSession>
    fun findByIpAddressAndIsActive(ipAddress: String, isActive: Boolean): List<UserSession>
}