package com.kobe.blogpress_api.database

import com.kobe.blogpress_api.model.audit.AuditLog
import com.kobe.blogpress_api.model.user.RoleType
import org.bson.types.ObjectId
import java.time.DayOfWeek
import java.time.Instant


// Modèles de données pour les rapports
data class WeeklyActivityReport(
    val userId: ObjectId,
    val userEmail: String,
    val userFullName: String,
    val department: String,
    val roleType: RoleType,
    val startDate: Instant,
    val endDate: Instant,
    val totalActivities: Int,
    val successfulActivities: Int,
    val failedActivities: Int,
    val activitiesByDay: Map<String, Int>,
    val activitiesByType: Map<String, Int>,
    val summary: ActivitySummary,
    val activities: List<AuditLog>
)

data class DepartmentActivityReport(
    val department: String,
    val startDate: Instant,
    val endDate: Instant,
    val totalUsers: Int,
    val totalActivities: Int,
    val successfulActivities: Int,
    val failedActivities: Int,
    val userReports: List<WeeklyActivityReport>
)

data class ActivitySummary(
    val mostFrequentAction: String?,
    val mostModifiedResource: String?,
    val mostActiveDay: DayOfWeek?,
    val uniqueResources: Int,
    val uniqueActions: Int
)