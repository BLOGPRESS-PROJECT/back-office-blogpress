package com.kobe.blogpress_api.model.audit

import org.bson.types.ObjectId
import java.time.Instant

// Classes de support pour les informations détaillées
data class GeoLocation(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val country: String? = null,
    val region: String? = null,
    val city: String? = null,
    val timezone: String? = null
)

data class DeviceInfo(
    val deviceType: String? = null, // mobile, desktop, tablet
    val operatingSystem: String? = null,
    val browser: String? = null,
    val browserVersion: String? = null,
    val screenResolution: String? = null,
    val language: String? = null
)

data class SecurityContext(
    val authenticationMethod: String? = null, // password, token, sso
    val ipReputationScore: Double? = null,
    val isSuspiciousActivity: Boolean = false,
    val riskLevel: String? = null, // LOW, MEDIUM, HIGH
    val additionalSecurityFlags: List<String> = emptyList()
)

data class ReportPeriod(
    val startDate: Instant,
    val endDate: Instant,
    val type: PeriodType,
    val description: String
)

enum class PeriodType {
    CUSTOM, TODAY, YESTERDAY, LAST_7_DAYS, LAST_30_DAYS,
    THIS_WEEK, THIS_MONTH, THIS_YEAR, LAST_WEEK, LAST_MONTH, LAST_YEAR
}

data class DayActivity(
    val date: String,
    val totalActivities: Int,
    val successfulActivities: Int,
    val failedActivities: Int,
    val mostActiveHour: Int? = null,
    val uniqueActions: Set<String> = emptySet()
)

data class ActionSummary(
    val action: String,
    val count: Int,
    val successRate: Double,
    val lastOccurrence: Instant,
    val category: String
)

data class RiskActivity(
    val id: ObjectId,
    val action: String,
    val resource: String,
    val timestamp: Instant,
    val riskLevel: String,
    val riskReason: String,
    val ipAddress: String? = null
)


data class HourActivity(
    val hour: Int,
    val activityCount: Int,
    val uniqueUsers: Int
)

enum class SystemHealthStatus {
    EXCELLENT, GOOD, WARNING, CRITICAL
}