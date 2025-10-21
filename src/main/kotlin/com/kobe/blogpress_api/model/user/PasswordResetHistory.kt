package com.kobe.blogpress_api.model.user

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("password_reset_history")
data class PasswordResetHistory(
    @Id val id: ObjectId = ObjectId(),
    val userId: ObjectId,
    val adminId: ObjectId,
    val resetDate: Instant = Instant.now(),
    val reason: String,
    val newPasswordHash: String
)