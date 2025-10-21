package com.kobe.blogpress_api.model.user

import com.kobe.blogpress_api.model.audit.DeviceInfo
import com.kobe.blogpress_api.model.audit.GeoLocation
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("user_sessions")
data class UserSession(
    @Id val id: ObjectId = ObjectId(),
    val sessionId: String,
    val userId: ObjectId,
    val ipAddress: String,
    val userAgent: String,
    val startTime: Instant = Instant.now(),
    val lastActivity: Instant = Instant.now(),
    val endTime: Instant? = null,
    val isActive: Boolean = true,
    val geoLocation: GeoLocation? = null,
    val deviceInfo: DeviceInfo? = null,
    val activitiesCount: Int = 0
)