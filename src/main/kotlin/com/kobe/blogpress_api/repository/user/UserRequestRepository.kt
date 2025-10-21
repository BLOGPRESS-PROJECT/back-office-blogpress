package com.kobe.blogpress_api.repository.user

import com.kobe.blogpress_api.dto.user.RequestStatus
import com.kobe.blogpress_api.dto.user.RequestType
import com.kobe.blogpress_api.dto.user.UserRequest
import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import java.time.Instant


interface UserRequestRepository : MongoRepository<UserRequest, ObjectId> {
    fun findByUserId(userId: ObjectId): List<UserRequest>
    fun findByUserIdAndStatus(userId: ObjectId, status: RequestStatus): List<UserRequest>
    fun findByStatus(status: RequestStatus): List<UserRequest>
    fun findByStatus(status: RequestStatus, pageable: Pageable): Page<UserRequest>
    fun findByRequestType(requestType: RequestType): List<UserRequest>
    fun findByRequestTypeAndStatus(requestType: RequestType, status: RequestStatus): List<UserRequest>
    fun findByCreatedAtBetween(startDate: Instant, endDate: Instant): List<UserRequest>
    fun countByStatus(status: RequestStatus): Long
    fun countByRequestType(requestType: RequestType): Long
}