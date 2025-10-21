package com.kobe.blogpress_api.dto.audit

import com.kobe.blogpress_api.model.audit.ActionSummary
import com.kobe.blogpress_api.model.audit.DayActivity
import com.kobe.blogpress_api.model.audit.HourActivity
import com.kobe.blogpress_api.model.audit.ReportPeriod
import com.kobe.blogpress_api.model.audit.RiskActivity
import com.kobe.blogpress_api.model.audit.SystemHealthStatus
import org.bson.types.ObjectId
import java.time.Instant

// DTOs pour les rapports d'activité
data class ActivityReportRequest(
    val userId: ObjectId? = null,
    val startDate: Instant,
    val endDate: Instant,
    val actions: List<String> = emptyList(),
    val resources: List<String> = emptyList(),
    val successOnly: Boolean? = null,
    val includeDetails: Boolean = false,
    val page: Int = 0,
    val size: Int = 50
)

data class ActivityReportResponse(
    val userId: ObjectId? = null,
    val userEmail: String? = null,
    val userFullName: String? = null,
    val department: String? = null,
    val reportPeriod: ReportPeriod,
    val totalActivities: Int,
    val successfulActivities: Int,
    val failedActivities: Int,
    val activitiesByDay: Map<String, DayActivity>,
    val activitiesByAction: Map<String, Int>,
    val activitiesByResource: Map<String, Int>,
    val topActions: List<ActionSummary>,
    val riskActivities: List<RiskActivity>,
    val activities: List<AuditLogSummary>,
    val metadata: ReportMetadata
)


data class AuditLogSummary(
    val id: ObjectId,
    val action: String,
    val resource: String,
    val resourceId: String? = null,
    val timestamp: Instant,
    val success: Boolean,
    val ipAddress: String? = null,
    val description: String? = null,
    val riskLevel: String? = null
)

data class ReportMetadata(
    val generatedAt: Instant,
    val generatedBy: ObjectId,
    val totalPages: Int,
    val currentPage: Int,
    val reportId: String,
    val filters: Map<String, Any> = emptyMap()
)

// DTO pour les rapports de département
data class DepartmentActivityReport(
    val department: String,
    val reportPeriod: ReportPeriod,
    val totalUsers: Int,
    val activeUsers: Int,
    val totalActivities: Int,
    val successfulActivities: Int,
    val failedActivities: Int,
    val topUsers: List<UserActivitySummary>,
    val topActions: List<ActionSummary>,
    val riskActivities: List<RiskActivity>,
    val complianceScore: Double,
    val userReports: List<ActivityReportResponse>
)

data class UserActivitySummary(
    val userId: ObjectId,
    val userEmail: String,
    val userFullName: String,
    val totalActivities: Int,
    val successRate: Double,
    val lastActivity: Instant,
    val riskScore: Double
)

// DTO pour les statistiques système
data class SystemActivityStats(
    val reportPeriod: ReportPeriod,
    val totalUsers: Int,
    val activeUsers: Int,
    val totalActivities: Int,
    val activitiesByCategory: Map<String, Int>,
    val peakActivityHours: List<HourActivity>,
    val securityAlerts: Int,
    val complianceScore: Double,
    val systemHealth: SystemHealthStatus
)


// DTO pour la recherche d'audit
data class AuditSearchRequest(
    val userId: ObjectId? = null,
    val actions: List<String> = emptyList(),
    val resources: List<String> = emptyList(),
    val startDate: Instant? = null,
    val endDate: Instant? = null,
    val ipAddress: String? = null,
    val success: Boolean? = null,
    val riskLevel: String? = null,
    val searchText: String? = null,
    val page: Int = 0,
    val size: Int = 20,
    val sortBy: String = "timestamp",
    val sortDirection: String = "DESC"
)