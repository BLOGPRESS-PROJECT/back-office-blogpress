package com.kobe.hrs.services.audit

import com.kobe.blogpress_api.model.audit.AuditLog
import com.kobe.blogpress_api.model.audit.DeviceInfo
import com.kobe.blogpress_api.model.audit.GeoLocation
import com.kobe.hrs.dto.audit.AuditLogSummary
import com.kobe.hrs.model.audit.AuditLog
import com.kobe.hrs.model.audit.DeviceInfo
import com.kobe.hrs.model.audit.GeoLocation

// Services de support (interfaces)
interface GeoLocationService {
    fun getLocation(ipAddress: String): GeoLocation?
}

interface DeviceDetectionService {
    fun detectDevice(userAgent: String): DeviceInfo?
}

// Extension pour convertir AuditLog en AuditLogSummary
fun AuditLog.toSummary(): AuditLogSummary {
    return AuditLogSummary(
        id = this.id,
        action = this.action,
        resource = this.resource,
        resourceId = this.resourceId,
        timestamp = this.timestamp,
        success = this.success,
        ipAddress = this.ipAddress,
        description = this.description,
        riskLevel = this.securityContext?.riskLevel
    )
}