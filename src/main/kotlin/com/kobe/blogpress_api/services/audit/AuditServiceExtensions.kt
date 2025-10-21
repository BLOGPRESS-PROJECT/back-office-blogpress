package com.kobe.hrs.services.audit

import com.kobe.hrs.dto.audit.AuditLogSummary
import com.kobe.hrs.dto.audit.AuditSearchRequest
import com.kobe.hrs.model.audit.AuditLog
import com.kobe.hrs.model.users.UserSession
import com.kobe.hrs.repository.audit.ExtendedAuditLogRepository
import com.kobe.hrs.repository.authRep.UserSessionRepository
import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.Instant

// Extensions du service d'audit pour les nouvelles méthodes
@Service
class AuditServiceExtensions(
    private val auditLogRepository: ExtendedAuditLogRepository,
    private val userSessionRepository: UserSessionRepository
) {
    fun searchAuditLogs(searchRequest: AuditSearchRequest): Page<AuditLogSummary> {
        // Ici vous implémenteriez la logique de recherche complexe
        // Pour l'instant, une version simplifiée
        val activities = auditLogRepository.findAll()
            .filter { activity ->
                var matches = true

                searchRequest.userId?.let { userId ->
                    matches = matches && activity.userId == userId
                }

                if (searchRequest.actions.isNotEmpty()) {
                    matches = matches && activity.action in searchRequest.actions
                }

                if (searchRequest.resources.isNotEmpty()) {
                    matches = matches && activity.resource in searchRequest.resources
                }

                searchRequest.startDate?.let { startDate ->
                    matches = matches && activity.timestamp.isAfter(startDate)
                }

                searchRequest.endDate?.let { endDate ->
                    matches = matches && activity.timestamp.isBefore(endDate)
                }

                searchRequest.ipAddress?.let { ip ->
                    matches = matches && activity.ipAddress == ip
                }

                searchRequest.success?.let { success ->
                    matches = matches && activity.success == success
                }

                searchRequest.riskLevel?.let { riskLevel ->
                    matches = matches && activity.securityContext?.riskLevel == riskLevel
                }

                searchRequest.searchText?.let { searchText ->
                    matches = matches && (
                            activity.action.contains(searchText, ignoreCase = true) ||
                                    activity.resource.contains(searchText, ignoreCase = true) ||
                                    activity.description?.contains(searchText, ignoreCase = true) == true
                            )
                }

                matches
            }
            .sortedWith(
                if (searchRequest.sortDirection == "DESC") {
                    compareByDescending { it.timestamp }
                } else {
                    compareBy { it.timestamp }
                }
            )

        val totalElements = activities.size
        val startIndex = searchRequest.page * searchRequest.size
        val endIndex = minOf(startIndex + searchRequest.size, totalElements)

        val pagedActivities = if (startIndex < totalElements) {
            activities.subList(startIndex, endIndex).map { it.toSummary() }
        } else emptyList()

        return PageImpl(
            pagedActivities,
            PageRequest.of(searchRequest.page, searchRequest.size),
            totalElements.toLong()
        )
    }

    fun getActivityById(id: ObjectId): AuditLog? {
        return auditLogRepository.findById(id).orElse(null)
    }

    fun getUserActiveSessions(userId: ObjectId): List<UserSession> {
        return userSessionRepository.findByUserIdAndIsActive(userId, true)
    }

    fun getActivitiesBetween(startDate: Instant, endDate: Instant): List<AuditLog> {
        return auditLogRepository.findByTimestampBetween(startDate, endDate)
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
}