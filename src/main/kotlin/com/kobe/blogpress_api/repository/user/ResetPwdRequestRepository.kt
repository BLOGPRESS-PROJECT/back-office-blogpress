package com.kobe.blogpress_api.repository.user

import com.kobe.blogpress_api.model.user.ResetPwdRequest
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface ResetPwdRequestRepository : MongoRepository<ResetPwdRequest, ObjectId> {
    fun findByUserId(userId: ObjectId): List<ResetPwdRequest>
}