package com.kobe.blogpress_api.services.audit

import com.kobe.blogpress_api.database.ActivitySummary
import com.kobe.blogpress_api.database.DepartmentActivityReport
import com.kobe.blogpress_api.database.WeeklyActivityReport
import com.kobe.blogpress_api.model.audit.AuditLog
import com.kobe.blogpress_api.repository.audit.AuditLogRepository
import com.kobe.blogpress_api.repository.user.UserRepository
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Service
class AuditService(
    private val auditLogRepository: AuditLogRepository,
    private val userRepository: UserRepository
) {
    fun log(
        userId: ObjectId,
        action: String,
        resource: String,
        resourceId: String? = null,
        details: Map<String, Any> = emptyMap(),
        ipAddress: String? = null,
        userAgent: String? = null,
        success: Boolean = true,
        description: String? = null,
    ) {
        val auditLog = AuditLog(
            userId = userId,
            action = action,
            resource = resource,
            resourceId = resourceId,
            details = details,
            ipAddress = ipAddress,
            userAgent = userAgent,
            success = success,
            description = description
        )
        auditLogRepository.save(auditLog)
    }


    fun generateWeeklyActivityReport(userId: ObjectId): WeeklyActivityReport {
        val user = userRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé")
        }

        val endDate = Instant.now()
        val startDate = endDate.minus(7, ChronoUnit.DAYS)

        val activities = auditLogRepository.findByUserIdAndTimestampBetween(
            userId, startDate, endDate
        ).sortedByDescending { it.timestamp }

        val summary = generateActivitySummary(activities)

        return WeeklyActivityReport(
            userId = userId,
            userEmail = user.email,
            userFullName = "${user.firstName} ${user.lastName}",
            department = user.department,
            roleType = user.roleType,
            startDate = startDate,
            endDate = endDate,
            totalActivities = activities.size,
            successfulActivities = activities.count { it.success },
            failedActivities = activities.count { !it.success },
            activitiesByDay = groupActivitiesByDay(activities),
            activitiesByType = groupActivitiesByType(activities),
            summary = summary,
            activities = activities
        )
    }

    // Génération de rapport pour tous les utilisateurs
    fun generateAllUsersWeeklyReport(): List<WeeklyActivityReport> {
        val activeUsers = userRepository.findByIsActive(true)
        return activeUsers.map { user ->
            generateWeeklyActivityReport(user.id)
        }
    }

    // Génération de rapport par département
    fun generateDepartmentWeeklyReport(department: String): DepartmentActivityReport {

        val departmentUsers = userRepository.findByDepartmentAndIsActive(department, true)
        val userReports = departmentUsers.map { user ->
            generateWeeklyActivityReport(user.id)
        }

        val totalActivities = userReports.sumOf { it.totalActivities }
        val totalSuccessful = userReports.sumOf { it.successfulActivities }
        val totalFailed = userReports.sumOf { it.failedActivities }

        return DepartmentActivityReport(
            department = department,
            startDate = Instant.now().minus(7, ChronoUnit.DAYS),
            endDate = Instant.now(),
            totalUsers = departmentUsers.size,
            totalActivities = totalActivities,
            successfulActivities = totalSuccessful,
            failedActivities = totalFailed,
            userReports = userReports
        )
    }

    private fun generateActivitySummary(activities: List<AuditLog>): ActivitySummary {
        val actionCounts = activities.groupingBy { it.action }.eachCount()
        val resourceCounts = activities.groupingBy { it.resource }.eachCount()
        val mostActiveDay = activities.groupingBy {
            it.timestamp.atZone(ZoneId.systemDefault()).dayOfWeek
        }.eachCount().maxByOrNull { it.value }?.key

        return ActivitySummary(
            mostFrequentAction = actionCounts.maxByOrNull { it.value }?.key,
            mostModifiedResource = resourceCounts.maxByOrNull { it.value }?.key,
            mostActiveDay = mostActiveDay,
            uniqueResources = resourceCounts.size,
            uniqueActions = actionCounts.size
        )
    }

    private fun groupActivitiesByDay(activities: List<AuditLog>): Map<String, Int> {
        return activities.groupingBy {
            it.timestamp.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        }.eachCount()
    }

    private fun groupActivitiesByType(activities: List<AuditLog>): Map<String, Int> {
        return activities.groupingBy { it.action }.eachCount()
    }
}