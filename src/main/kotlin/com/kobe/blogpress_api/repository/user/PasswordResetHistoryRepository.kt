package com.kobe.blogpress_api.repository.user

import com.kobe.blogpress_api.model.user.PasswordResetHistory
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface PasswordResetHistoryRepository : MongoRepository<PasswordResetHistory, ObjectId> {
    fun findByUserId(userId: ObjectId): List<PasswordResetHistory>
}