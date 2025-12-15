package com.kobe.blogpress_api.services.user

import com.kobe.blogpress_api.domain.model.user.Role
import com.kobe.blogpress_api.domain.model.user.User
import com.kobe.blogpress_api.dto.user.PrivacyPreferencesDTO
import com.kobe.blogpress_api.dto.user.UpdateProfileRequestDTO
import com.kobe.blogpress_api.dto.user.UserDTO
import com.kobe.blogpress_api.exception.ResourceNotFoundException
import com.kobe.blogpress_api.repository.blog.BlogRepository
import com.kobe.blogpress_api.repository.article.ArticleRepository
import com.kobe.blogpress_api.repository.user.UserRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate

@Service
class UserService(
    private val userRepository: UserRepository,
    private val blogRepository: BlogRepository,
    private val articleRepository: ArticleRepository
) {

    suspend fun findById(userId: ObjectId): User {
        return userRepository.findById(userId).awaitSingleOrNull()
            ?: throw ResourceNotFoundException("User not found with id: ${userId.toHexString()}")
    }

    suspend fun findByUsername(username: String): User {
        return userRepository.findByUsername(username).awaitSingleOrNull()
            ?: throw ResourceNotFoundException("User not found with username: $username")
    }

    suspend fun updateProfile(userId: ObjectId, updateRequest: UpdateProfileRequestDTO): User {
        val user = findById(userId)

        val updatedUser = user.copy(
            firstName = updateRequest.firstName ?: user.firstName,
            lastName = updateRequest.lastName ?: user.lastName,
            birthDate = updateRequest.birthDate ?: user.birthDate,
            gender = updateRequest.gender ?: user.gender,
            country = updateRequest.country ?: user.country,
            phoneNumber = updateRequest.phoneNumber ?: user.phoneNumber,
            interests = updateRequest.interests ?: user.interests,
            preferredLanguage = updateRequest.preferredLanguage ?: user.preferredLanguage,
            bio = updateRequest.bio ?: user.bio,
            website = updateRequest.website ?: user.website,
            socialLinks = updateRequest.socialLinks ?: user.socialLinks,
            updatedAt = Instant.now()
        )

        return userRepository.save(updatedUser).awaitSingle()
    }

    suspend fun updateProfilePicture(userId: ObjectId, profilePictureUrl: String): User {
        val user = findById(userId)

        val updatedUser = user.copy(
            profilePicture = profilePictureUrl,
            updatedAt = Instant.now()
        )

        return userRepository.save(updatedUser).awaitSingle()
    }

    suspend fun followUser(followerId: ObjectId, followingId: ObjectId): Pair<User, User> {
        if (followerId == followingId) {
            throw IllegalArgumentException("Cannot follow yourself")
        }

        val follower = findById(followerId)
        val following = findById(followingId)

        if (follower.following.contains(followingId)) {
            throw IllegalStateException("Already following this user")
        }

        val updatedFollower = follower.copy(
            following = follower.following + followingId,
            statistics = follower.statistics.copy(
                followingCount = follower.statistics.followingCount + 1
            ),
            updatedAt = Instant.now()
        )

        val updatedFollowing = following.copy(
            followers = following.followers + followerId,
            statistics = following.statistics.copy(
                followerCount = following.statistics.followerCount + 1
            ),
            updatedAt = Instant.now()
        )

        val savedFollower = userRepository.save(updatedFollower).awaitSingle()
        val savedFollowing = userRepository.save(updatedFollowing).awaitSingle()

        return Pair(savedFollower, savedFollowing)
    }

    suspend fun unfollowUser(followerId: ObjectId, followingId: ObjectId): Pair<User, User> {
        if (followerId == followingId) {
            throw IllegalArgumentException("Cannot unfollow yourself")
        }

        val follower = findById(followerId)
        val following = findById(followingId)

        if (!follower.following.contains(followingId)) {
            throw IllegalStateException("Not following this user")
        }

        val updatedFollower = follower.copy(
            following = follower.following - followingId,
            statistics = follower.statistics.copy(
                followingCount = maxOf(0, follower.statistics.followingCount - 1)
            ),
            updatedAt = Instant.now()
        )

        val updatedFollowing = following.copy(
            followers = following.followers - followerId,
            statistics = following.statistics.copy(
                followerCount = maxOf(0, following.statistics.followerCount - 1)
            ),
            updatedAt = Instant.now()
        )

        val savedFollower = userRepository.save(updatedFollower).awaitSingle()
        val savedFollowing = userRepository.save(updatedFollowing).awaitSingle()

        return Pair(savedFollower, savedFollowing)
    }

    // Dans UserService.kt
    suspend fun isFollowing(followerId: ObjectId, followingId: ObjectId): Boolean {
        val follower = findById(followerId)
        return follower.following.contains(followingId)
    }

    // Promouvoir un utilisateur en Golden User (ADMIN seulement)
    suspend fun promoteToGoldenUser(userId: ObjectId, adminId: ObjectId): User {
        // Vérifier que l'admin a le droit
        val admin = findById(adminId)
        if (admin.role != Role.ADMIN) {
            throw IllegalArgumentException("Only admins can promote users to Golden status")
        }

        val user = findById(userId)

        if (user.isGoldenUser) {
            throw IllegalStateException("User is already a Golden User")
        }

        val updatedUser = user.copy(
            isGoldenUser = true,
            goldenUserSince = Instant.now(),
            updatedAt = Instant.now()
        )

        return userRepository.save(updatedUser).awaitSingle()
    }

    // Révoquer le statut Golden User (ADMIN seulement)
    suspend fun revokeGoldenUser(userId: ObjectId, adminId: ObjectId): User {
        // Vérifier que l'admin a le droit
        val admin = findById(adminId)
        if (admin.role != Role.ADMIN) {
            throw IllegalArgumentException("Only admins can revoke Golden status")
        }

        val user = findById(userId)

        if (!user.isGoldenUser) {
            throw IllegalStateException("User is not a Golden User")
        }

        val updatedUser = user.copy(
            isGoldenUser = false,
            goldenUserSince = null,
            updatedAt = Instant.now()
        )

        return userRepository.save(updatedUser).awaitSingle()
    }

    // Vérifier si un utilisateur est Golden
    suspend fun isGoldenUser(userId: ObjectId): Boolean {
        val user = findById(userId)
        return user.isGoldenUser
    }

    suspend fun searchUsers(query: String, page: Int, size: Int): Page<User> {
        val regex = Regex(query, RegexOption.IGNORE_CASE)
        return userRepository.findByUsernameOrEmailOrFullName(regex, PageRequest.of(page, size))
    }


     suspend fun updatePrivacyPreferences(
         userId: ObjectId,
         preferences: PrivacyPreferencesDTO
     ): User {
         val user = findById(userId)
         val updatedUser = user.copy(
             isPublic = preferences.isPublic,
             showEmail = preferences.showEmail,
             showLocation = preferences.showLocation,
             updatedAt = Instant.now()
         )
         return userRepository.save(updatedUser).awaitSingle()
     }

    // ⭐ NOUVEAU : Calculer les statistiques à la volée pour s'assurer qu'elles sont à jour
    suspend fun calculateUserStatistics(userId: ObjectId): com.kobe.blogpress_api.domain.model.user.UserStatistics {
        val totalBlogs = blogRepository.countByAuthorId(userId).awaitSingle()
        val totalPosts = articleRepository.countByAuthorId(userId).awaitSingle()
        
        val user = findById(userId)
        
        // Utiliser les statistiques existantes pour les autres champs (followers, following, etc.)
        // et mettre à jour totalBlogs et totalPosts
        return user.statistics.copy(
            totalBlogs = totalBlogs,
            totalPosts = totalPosts,
            followerCount = user.followers.size.toLong(),
            followingCount = user.following.size.toLong()
        )
    }

    /**
     * Récupérer tous les utilisateurs paginés (pour l'admin).
     */
    suspend fun findAllUsers(pageable: org.springframework.data.domain.Pageable): Page<User> {
        val users = userRepository.findAll(pageable as Sort).collectList().awaitSingle()
        val total = userRepository.count().awaitSingle()
        return PageImpl(users, pageable, total)
    }

    // Ajoute cette méthode
    suspend fun toDTO(user: User): UserDTO {
        // ⭐ Calculer les statistiques à la volée pour s'assurer qu'elles sont à jour
        val updatedStatistics = calculateUserStatistics(user.id)
        
        return UserDTO(
            id = user.id.toHexString(),
            username = user.username,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            fullName = "${user.firstName} ${user.lastName}",
            birthDate = user.birthDate,
            age = user.birthDate?.let { calculateAge(it) },
            gender = user.gender,
            isGoldenUser = user.isGoldenUser,
            goldenUserSince = user.goldenUserSince,
            country = user.country,
            phoneNumber = user.phoneNumber,
            interests = user.interests,
            preferredLanguage = user.preferredLanguage,
            profilePicture = user.profilePicture,
            bio = user.bio,
            website = user.website,
            socialLinks = user.socialLinks,
            role = user.role,
            isEmailVerified = user.isEmailVerified,
            statistics = updatedStatistics, // ⭐ Utiliser les statistiques calculées
            createdAt = user.createdAt,
            lastLoginAt = user.lastLoginAt
        )
    }

    // Ajoute cette méthode helper
    private fun calculateAge(birthDate: LocalDate): Int {
        return java.time.Period.between(birthDate, LocalDate.now()).years
    }
}