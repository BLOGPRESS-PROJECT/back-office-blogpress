package com.kobe.blogpress_api.services.user

import com.kobe.blogpress_api.domain.model.user.User
import com.kobe.blogpress_api.dto.user.UpdateProfileRequestDTO
import com.kobe.blogpress_api.dto.user.UserDTO
import com.kobe.blogpress_api.exception.ResourceNotFoundException
import com.kobe.blogpress_api.repository.user.UserRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate

@Service
class UserService(
    private val userRepository: UserRepository
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

    // Ajoute cette méthode
    fun toDTO(user: User): UserDTO {
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
            statistics = user.statistics,
            createdAt = user.createdAt,
            lastLoginAt = user.lastLoginAt
        )
    }

    // Ajoute cette méthode helper
    private fun calculateAge(birthDate: LocalDate): Int {
        return java.time.Period.between(birthDate, LocalDate.now()).years
    }
}