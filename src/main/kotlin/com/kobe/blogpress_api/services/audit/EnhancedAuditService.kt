package com.kobe.hrs.services.audit

import com.fasterxml.jackson.databind.ObjectMapper
import com.kobe.blogpress_api.dto.audit.ActivityReportRequest
import com.kobe.blogpress_api.dto.audit.ActivityReportResponse
import com.kobe.blogpress_api.dto.audit.ReportMetadata
import com.kobe.blogpress_api.dto.audit.UserActivitySummary
import com.kobe.blogpress_api.model.audit.ActionSummary
import com.kobe.blogpress_api.model.audit.AuditLog
import com.kobe.blogpress_api.model.audit.DayActivity
import com.kobe.blogpress_api.model.audit.PeriodType
import com.kobe.blogpress_api.model.audit.ReportPeriod
import com.kobe.blogpress_api.model.audit.RiskActivity
import com.kobe.blogpress_api.model.audit.SecurityContext
import com.kobe.blogpress_api.repository.audit.ExtendedAuditLogRepository
import com.kobe.blogpress_api.repository.user.UserRepository
import com.kobe.blogpress_api.repository.user.UserSessionRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class EnhancedAuditService(
    private val auditLogRepository: ExtendedAuditLogRepository,
    private val userRepository: UserRepository,
    private val userSessionRepository: UserSessionRepository,
    private val geoLocationService: GeoLocationService? = null,
    private val deviceDetectionService: DeviceDetectionService? = null
) {
    private val logger = LoggerFactory.getLogger(EnhancedAuditService::class.java)
    private val objectMapper = ObjectMapper()

    /**
     * Méthode principale de logging avec tous les paramètres possibles
     */
    fun logComplete(
        userId: ObjectId,
        action: String,
        resource: String,
        resourceId: String? = null,
        details: Map<String, Any> = emptyMap(),
        ipAddress: String? = null,
        userAgent: String? = null,
        success: Boolean = true,
        description: String? = null,
        sessionId: String? = null,
        requestId: String? = null,
        httpMethod: String? = null,
        endpoint: String? = null,
        requestHeaders: Map<String, String> = emptyMap(),
        requestParameters: Map<String, Any> = emptyMap(),
        requestBody: String? = null,
        responseStatus: Int? = null,
        responseBody: String? = null,
        executionTimeMs: Long? = null,
        errorMessage: String? = null,
        stackTrace: String? = null
    ) {
        try {
            val geoLocation = ipAddress?.let { geoLocationService?.getLocation(it) }
            val deviceInfo = userAgent?.let { deviceDetectionService?.detectDevice(it) }
            val securityContext = analyzeSecurityContext(userId, ipAddress, action, resource)

            val auditLog = AuditLog(
                userId = userId,
                action = action,
                resource = resource,
                resourceId = resourceId,
                details = details,
                ipAddress = ipAddress,
                userAgent = userAgent,
                success = success,
                description = description,
                sessionId = sessionId,
                requestId = requestId,
                httpMethod = httpMethod,
                endpoint = endpoint,
                requestHeaders = sanitizeHeaders(requestHeaders),
                requestParameters = requestParameters,
                requestBody = sanitizeRequestBody(requestBody),
                responseStatus = responseStatus,
                responseBody = sanitizeResponseBody(responseBody),
                executionTimeMs = executionTimeMs,
                errorMessage = errorMessage,
                stackTrace = stackTrace,
                geoLocation = geoLocation,
                deviceInfo = deviceInfo,
                securityContext = securityContext,
                dataHash = generateDataHash(userId, action, resource, resourceId, details)
            )

            auditLogRepository.save(auditLog)

            // Mettre à jour les statistiques de session si disponible
            sessionId?.let { updateSessionActivity(it) }

            // Analyser les activités suspectes
            if (securityContext?.isSuspiciousActivity == true) {
                handleSuspiciousActivity(auditLog)
            }

        } catch (e: Exception) {
            logger.error("Erreur lors de l'enregistrement du log d'audit", e)
            // En cas d'erreur, on enregistre un log minimal
            saveMinimalAuditLog(userId, action, resource, success, e.message)
        }
    }

    /**
     * Méthode simplifiée pour compatibilité avec l'existant
     */
    fun log(
        userId: ObjectId,
        action: String,
        resource: String,
        resourceId: String? = null,
        details: Map<String, Any> = emptyMap(),
        ipAddress: String? = null,
        userAgent: String? = null,
        success: Boolean = true,
        description: String? = null
    ) {
        logComplete(
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
    }

    /**
     * Log complet à partir de la requête HTTP
     */
    fun logFromHttpRequest(
        userId: ObjectId,
        action: String,
        resource: String,
        resourceId: String? = null,
        details: Map<String, Any> = emptyMap(),
        request: HttpServletRequest,
        response: HttpServletResponse? = null,
        success: Boolean = true,
        description: String? = null,
        executionTimeMs: Long? = null,
        requestBody: String? = null,
        responseBody: String? = null,
        errorMessage: String? = null
    ) {
        val sessionId = request.session?.id
        val requestId = request.getHeader("X-Request-ID") ?: UUID.randomUUID().toString()

        logComplete(
            userId = userId,
            action = action,
            resource = resource,
            resourceId = resourceId,
            details = details,
            ipAddress = getClientIpAddress(request),
            userAgent = request.getHeader("User-Agent"),
            success = success,
            description = description,
            sessionId = sessionId,
            requestId = requestId,
            httpMethod = request.method,
            endpoint = request.requestURI,
            requestHeaders = extractHeaders(request),
            requestParameters = extractParameters(request),
            requestBody = requestBody,
            responseStatus = response?.status,
            responseBody = responseBody,
            executionTimeMs = executionTimeMs
        )
    }

    /**
     * Génération de rapport d'activité pour une période donnée
     */
    fun generateActivityReport(request: ActivityReportRequest): ActivityReportResponse {
        val activities = if (request.userId != null) {
            auditLogRepository.findByUserIdAndTimestampBetween(
                request.userId, request.startDate, request.endDate
            )
        } else {
            auditLogRepository.findByTimestampBetween(request.startDate, request.endDate)
        }

        val filteredActivities = filterActivities(activities, request)
        val paginatedActivities = paginateActivities(filteredActivities, request.page, request.size)

        val user = request.userId?.let { userId ->
            userRepository.findById(userId).orElse(null)
        }

        return ActivityReportResponse(
            userId = request.userId,
            userEmail = user?.email,
            userFullName = user?.let { "${it.firstName} ${it.lastName}" },
            department = user?.department,
            reportPeriod = ReportPeriod(
                startDate = request.startDate,
                endDate = request.endDate,
                type = determinePeriodType(request.startDate, request.endDate),
                description = formatPeriodDescription(request.startDate, request.endDate)
            ),
            totalActivities = filteredActivities.size,
            successfulActivities = filteredActivities.count { it.success },
            failedActivities = filteredActivities.count { !it.success },
            activitiesByDay = groupActivitiesByDay(filteredActivities),
            activitiesByAction = groupActivitiesByAction(filteredActivities),
            activitiesByResource = groupActivitiesByResource(filteredActivities),
            topActions = generateTopActions(filteredActivities),
            riskActivities = extractRiskActivities(filteredActivities),
            activities = paginatedActivities.map { it.toSummary() },
            metadata = ReportMetadata(
                generatedAt = Instant.now(),
                generatedBy = getCurrentUserId(),
                totalPages = calculateTotalPages(filteredActivities.size, request.size),
                currentPage = request.page,
                reportId = generateReportId(),
                filters = mapOf(
                    "actions" to request.actions,
                    "resources" to request.resources,
                    "successOnly" to request.successOnly
                ) as Map<String, Any>
            )
        )
    }

    /**
     * Rapport d'activité pour une journée spécifique
     */
    fun getDailyActivityReport(userId: ObjectId, date: LocalDate): ActivityReportResponse {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        return generateActivityReport(
            ActivityReportRequest(
                userId = userId,
                startDate = startOfDay,
                endDate = endOfDay,
                includeDetails = true
            )
        )
    }

    /**
     * Rapport d'activité pour la semaine dernière
     */
    fun getWeeklyActivityReport(userId: ObjectId): ActivityReportResponse {
        val endDate = Instant.now()
        val startDate = endDate.minus(7, ChronoUnit.DAYS)

        return generateActivityReport(
            ActivityReportRequest(
                userId = userId,
                startDate = startDate,
                endDate = endDate,
                includeDetails = true
            )
        )
    }

    /**
     * Rapport d'activité pour le mois dernier
     */
    fun getMonthlyActivityReport(userId: ObjectId): ActivityReportResponse {
        val endDate = Instant.now()
        val startDate = endDate.minus(30, ChronoUnit.DAYS)

        return generateActivityReport(
            ActivityReportRequest(
                userId = userId,
                startDate = startDate,
                endDate = endDate,
                includeDetails = true
            )
        )
    }

    /**
     * Rapport d'activité pour l'année dernière
     */
    fun getYearlyActivityReport(userId: ObjectId): ActivityReportResponse {
        val endDate = Instant.now()
        val startDate = endDate.minus(365, ChronoUnit.DAYS)

        return generateActivityReport(
            ActivityReportRequest(
                userId = userId,
                startDate = startDate,
                endDate = endDate,
                includeDetails = false
            )
        )
    }

    // Méthodes utilitaires privées

    private fun sanitizeHeaders(headers: Map<String, String>): Map<String, String> {
        val sensitiveHeaders = setOf("authorization", "cookie", "x-api-key", "x-auth-token")
        return headers.filterKeys { key ->
            !sensitiveHeaders.contains(key.lowercase())
        }
    }

    private fun sanitizeRequestBody(body: String?): String? {
        return body?.let {
            if (it.length > 10000) it.substring(0, 10000) + "..." else it
        }
    }

    private fun sanitizeResponseBody(body: String?): String? {
        return body?.let {
            if (it.length > 5000) it.substring(0, 5000) + "..." else it
        }
    }

    private fun generateDataHash(
        userId: ObjectId,
        action: String,
        resource: String,
        resourceId: String?,
        details: Map<String, Any>
    ): String {
        val data = "$userId-$action-$resource-$resourceId-${details.hashCode()}"
        return MessageDigest.getInstance("SHA-256")
            .digest(data.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun analyzeSecurityContext(
        userId: ObjectId,
        ipAddress: String?,
        action: String,
        resource: String
    ): SecurityContext {
        // Logique d'analyse de sécurité
        val riskLevel = calculateRiskLevel(userId, ipAddress, action, resource)
        val isSuspicious = detectSuspiciousActivity(userId, ipAddress, action)

        return SecurityContext(
            authenticationMethod = "session", // À adapter selon votre système
            riskLevel = riskLevel,
            isSuspiciousActivity = isSuspicious
        )
    }

    private fun calculateRiskLevel(userId: ObjectId, ipAddress: String?, action: String, resource: String): String {
        var riskScore = 0

        // Facteurs de risque
        if (action.contains("DELETE") || action.contains("DEACTIVATE")) riskScore += 3
        if (action.contains("EXPORT") || action.contains("DOWNLOAD")) riskScore += 2
        if (resource.contains("PAYROLL") || resource.contains("SENSITIVE")) riskScore += 2

        // Vérifier les activités récentes suspectes
        ipAddress?.let { ip ->
            val recentActivities = auditLogRepository.findByIpAddressAndTimestampBetween(
                ip, Instant.now().minus(1, ChronoUnit.HOURS), Instant.now()
            )
            if (recentActivities.size > 100) riskScore += 3
        }

        return when {
            riskScore >= 5 -> "HIGH"
            riskScore >= 3 -> "MEDIUM"
            else -> "LOW"
        }
    }

    private fun detectSuspiciousActivity(userId: ObjectId, ipAddress: String?, action: String): Boolean {
        // Détection d'activités suspectes
        val now = Instant.now()
        val lastHour = now.minus(1, ChronoUnit.HOURS)

        val recentActivities = auditLogRepository.findByUserIdAndTimestampAfter(userId, lastHour)

        // Trop d'activités dans la dernière heure
        if (recentActivities.size > 50) return true

        // Échecs de connexion répétés
        val failedLogins = recentActivities.count {
            it.action == "LOGIN_FAILED" && !it.success
        }
        if (failedLogins > 5) return true

        // Accès depuis une nouvelle IP
        ipAddress?.let { ip ->
            val historicalIps = auditLogRepository.findByUserIdAndTimestampAfter(
                userId, now.minus(30, ChronoUnit.DAYS)
            ).mapNotNull { it.ipAddress }.toSet()

            if (ip !in historicalIps) return true
        }

        return false
    }

    private fun updateSessionActivity(sessionId: String) {
        userSessionRepository.findBySessionId(sessionId)?.let { session ->
            userSessionRepository.save(
                session.copy(
                    lastActivity = Instant.now(),
                    activitiesCount = session.activitiesCount + 1
                )
            )
        }
    }

    private fun handleSuspiciousActivity(auditLog: AuditLog) {
        logger.warn("Activité suspecte détectée: userId=${auditLog.userId}, action=${auditLog.action}, ip=${auditLog.ipAddress}")
        // Ici vous pourriez envoyer des alertes, bloquer temporairement, etc.
    }

    private fun saveMinimalAuditLog(userId: ObjectId, action: String, resource: String, success: Boolean, errorMessage: String?) {
        try {
            val minimalLog = AuditLog(
                userId = userId,
                action = action,
                resource = resource,
                success = success,
                errorMessage = errorMessage
            )
            auditLogRepository.save(minimalLog)
        } catch (e: Exception) {
            logger.error("Impossible d'enregistrer même un log minimal", e)
        }
    }

    private fun getClientIpAddress(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) {
            return xForwardedFor.split(",")[0].trim()
        }

        val xRealIp = request.getHeader("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) {
            return xRealIp
        }

        return request.remoteAddr
    }

    private fun extractHeaders(request: HttpServletRequest): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        request.headerNames.asSequence().forEach { headerName ->
            headers[headerName] = request.getHeader(headerName)
        }
        return headers
    }

    private fun extractParameters(request: HttpServletRequest): Map<String, Any> {
        val parameters = mutableMapOf<String, Any>()
        request.parameterMap.forEach { (key, values) ->
            parameters[key] = if (values.size == 1) values[0] else values.toList()
        }
        return parameters
    }

    private fun filterActivities(activities: List<AuditLog>, request: ActivityReportRequest): List<AuditLog> {
        var filtered = activities

        if (request.actions.isNotEmpty()) {
            filtered = filtered.filter { it.action in request.actions }
        }

        if (request.resources.isNotEmpty()) {
            filtered = filtered.filter { it.resource in request.resources }
        }

        request.successOnly?.let { successOnly ->
            filtered = filtered.filter { it.success == successOnly }
        }

        return filtered.sortedByDescending { it.timestamp }
    }

    private fun paginateActivities(activities: List<AuditLog>, page: Int, size: Int): List<AuditLog> {
        val startIndex = page * size
        val endIndex = minOf(startIndex + size, activities.size)
        return if (startIndex < activities.size) {
            activities.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }

    private fun groupActivitiesByDay(activities: List<AuditLog>): Map<String, DayActivity> {
        return activities.groupBy {
            it.timestamp.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        }.mapValues { (date, dayActivities) ->
            val hourCounts = dayActivities.groupingBy {
                it.timestamp.atZone(ZoneId.systemDefault()).hour
            }.eachCount()

            DayActivity(
                date = date,
                totalActivities = dayActivities.size,
                successfulActivities = dayActivities.count { it.success },
                failedActivities = dayActivities.count { !it.success },
                mostActiveHour = hourCounts.maxByOrNull { it.value }?.key,
                uniqueActions = dayActivities.map { it.action }.toSet()
            )
        }
    }

    private fun groupActivitiesByAction(activities: List<AuditLog>): Map<String, Int> {
        return activities.groupingBy { it.action }.eachCount()
    }

    private fun groupActivitiesByResource(activities: List<AuditLog>): Map<String, Int> {
        return activities.groupingBy { it.resource }.eachCount()
    }

    private fun generateTopActions(activities: List<AuditLog>): List<ActionSummary> {
        return activities.groupBy { it.action }
            .map { (action, actionActivities) ->
                val successCount = actionActivities.count { it.success }
                val successRate = if (actionActivities.isNotEmpty()) {
                    successCount.toDouble() / actionActivities.size * 100
                } else 0.0

                val category = AuditLog.AuditActionType.fromString(action)?.category ?: "OTHER"

                ActionSummary(
                    action = action,
                    count = actionActivities.size,
                    successRate = successRate,
                    lastOccurrence = actionActivities.maxOf { it.timestamp },
                    category = category
                )
            }
            .sortedByDescending { it.count }
            .take(10)
    }

    private fun extractRiskActivities(activities: List<AuditLog>): List<RiskActivity> {
        return activities.filter {
            it.securityContext?.riskLevel in listOf("HIGH", "MEDIUM") ||
                    it.securityContext?.isSuspiciousActivity == true ||
                    !it.success
        }.map { activity ->
            RiskActivity(
                id = activity.id,
                action = activity.action,
                resource = activity.resource,
                timestamp = activity.timestamp,
                riskLevel = activity.securityContext?.riskLevel ?: "UNKNOWN",
                riskReason = determineRiskReason(activity),
                ipAddress = activity.ipAddress
            )
        }.sortedByDescending { it.timestamp }
    }

    private fun determineRiskReason(activity: AuditLog): String {
        val reasons = mutableListOf<String>()

        if (!activity.success) reasons.add("Échec d'opération")
        if (activity.securityContext?.isSuspiciousActivity == true) reasons.add("Activité suspecte")
        if (activity.action.contains("DELETE")) reasons.add("Opération de suppression")
        if (activity.action.contains("EXPORT")) reasons.add("Export de données")
        if (activity.securityContext?.riskLevel == "HIGH") reasons.add("Niveau de risque élevé")

        return reasons.joinToString(", ").ifEmpty { "Activité à risque" }
    }

    private fun generateTopUsers(userReports: List<ActivityReportResponse>): List<UserActivitySummary> {
        return userReports.mapNotNull { report ->
            report.userId?.let { userId ->
                UserActivitySummary(
                    userId = userId,
                    userEmail = report.userEmail ?: "",
                    userFullName = report.userFullName ?: "",
                    totalActivities = report.totalActivities,
                    successRate = if (report.totalActivities > 0) {
                        report.successfulActivities.toDouble() / report.totalActivities * 100
                    } else 0.0,
                    lastActivity = report.activities.maxOfOrNull { it.timestamp } ?: Instant.now(),
                    riskScore = calculateUserRiskScore(report.riskActivities)
                )
            }
        }.sortedByDescending { it.totalActivities }
    }

    private fun calculateUserRiskScore(riskActivities: List<RiskActivity>): Double {
        if (riskActivities.isEmpty()) return 0.0

        val highRiskCount = riskActivities.count { it.riskLevel == "HIGH" }
        val mediumRiskCount = riskActivities.count { it.riskLevel == "MEDIUM" }

        return (highRiskCount * 3.0 + mediumRiskCount * 1.5) / riskActivities.size
    }

    private fun calculateComplianceScore(activities: List<AuditLog>): Double {
        if (activities.isEmpty()) return 100.0

        val successfulActivities = activities.count { it.success }
        val riskActivities = activities.count {
            it.securityContext?.riskLevel in listOf("HIGH", "MEDIUM")
        }

        val baseScore = (successfulActivities.toDouble() / activities.size) * 100
        val riskPenalty = (riskActivities.toDouble() / activities.size) * 20

        return maxOf(0.0, baseScore - riskPenalty)
    }

    private fun determinePeriodType(startDate: Instant, endDate: Instant): PeriodType {
        val duration = Duration.between(startDate, endDate)
        return when {
            duration.toDays() == 1L -> PeriodType.TODAY
            duration.toDays() == 7L -> PeriodType.LAST_7_DAYS
            duration.toDays() == 30L -> PeriodType.LAST_30_DAYS
            duration.toDays() == 365L -> PeriodType.LAST_YEAR
            else -> PeriodType.CUSTOM
        }
    }

    private fun formatPeriodDescription(startDate: Instant, endDate: Instant): String {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        val start = startDate.atZone(ZoneId.systemDefault()).format(formatter)
        val end = endDate.atZone(ZoneId.systemDefault()).format(formatter)
        return "Du $start au $end"
    }

    private fun calculateTotalPages(totalItems: Int, pageSize: Int): Int {
        return if (totalItems == 0) 0 else (totalItems - 1) / pageSize + 1
    }

    private fun generateReportId(): String {
        return "RPT-${System.currentTimeMillis()}-${UUID.randomUUID().toString().substring(0, 8)}"
    }

    private fun getCurrentUserId(): ObjectId {
        // À implémenter selon votre système d'authentification
        return ObjectId() // Placeholder
    }
}
