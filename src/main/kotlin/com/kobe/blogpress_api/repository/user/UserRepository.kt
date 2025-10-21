package com.kobe.blogpress_api.repository.user

import com.kobe.blogpress_api.model.user.DeactivationReason
import com.kobe.blogpress_api.model.user.RoleType
import com.kobe.blogpress_api.model.user.User
import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface UserRepository: MongoRepository<User, ObjectId> {
    fun findByEmail(email: String): User?
    fun findByIsActive(isActive: Boolean): List<User>
    fun findByDepartmentAndIsActive(department: String, isActive: Boolean): List<User>
    fun findByIsActive(isActive: Boolean, pageable: Pageable): Page<User>
    fun findByDepartmentAndIsActive(department: String, isActive: Boolean, pageable: Pageable): Page<User>
    fun findByRoleType(roleType: RoleType): List<User>
    fun findByRoleTypeAndIsActive(roleType: RoleType, isActive: Boolean, pageable: Pageable): Page<User>

    fun countByIsActive(isActive: Boolean): Long
    fun countByRoleType(roleType: RoleType): Long
    fun countByRoleTypeAndIsActive(roleType: RoleType, isActive: Boolean): Long

    // Nouvelles fonctions pour la gestion des tentatives de connexion
    fun countByIsActiveTrue(): Long
    fun countByIsActiveFalse(): Long
    fun countByPermanentlyDisabledTrue(): Long
    fun countByBlockedUntilAfter(date: Instant): Long
    fun countByLoginAttemptsGreaterThan(attempts: Int): Long
    fun countByDeactivationReason(reason: DeactivationReason): Long

    // Fonctions pour la gestion des blocages
    fun findByBlockedUntilIsNotNullAndBlockedUntilAfter(date: Instant): List<User>
    fun findByLoginAttemptsGreaterThanAndBlockedUntilIsNull(attempts: Int): List<User>
    fun findByPermanentlyDisabledTrue(): List<User>
    fun findByLastFailedLoginAttemptAfter(date: Instant): List<User>

}