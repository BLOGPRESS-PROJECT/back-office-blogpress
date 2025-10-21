package com.kobe.blogpress_api.model.user

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("reset_pwd_requests")
data class ResetPwdRequest(
    @Id val id: ObjectId = ObjectId(),
    val userId: ObjectId,
    val userEmail: String,
    val date: Instant = Instant.now(),
    val motif: String,
    val status: ResetPwdStatus = ResetPwdStatus.PENDING
)

enum class ResetPwdStatus { PENDING, ACCEPTED, REJECTED }