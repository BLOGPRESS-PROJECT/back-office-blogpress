package com.kobe.blogpress_api.model.user

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("roles")
data class Role(
    @Id val id: ObjectId = ObjectId(),
    val roleType: RoleType,
    val permissions: Set<Permission>,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val isActive: Boolean = true
)