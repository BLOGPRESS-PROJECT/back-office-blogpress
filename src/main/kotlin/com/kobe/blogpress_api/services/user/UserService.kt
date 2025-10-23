package com.kobe.blogpress_api.services.user

import com.kobe.blogpress_api.domain.model.user.User
import com.kobe.blogpress_api.dto.user.UpdateProfileRequestDTO
import com.kobe.blogpress_api.dto.user.UserDTO
import com.kobe.blogpress_api.exception.ResourceNotFoundException
import com.kobe.blogpress_api.repository.user.UserRepository
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.util.function.Tuple2
import reactor.util.function.Tuples
import java.time.Instant

@Service
class UserService(
    private val userRepository: UserRepository
) {

    fun findById(userId: ObjectId): Mono<User> {
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(ResourceNotFoundException("User not found with id: ${userId.toHexString()}")))
    }

    fun findByUsername(username: String): Mono<User> {
        return userRepository.findByUsername(username)
            .switchIfEmpty(Mono.error(ResourceNotFoundException("User not found with username: $username")))
    }

    fun updateProfile(userId: ObjectId, updateRequest: UpdateProfileRequestDTO): Mono<User> {
        return findById(userId)
            .flatMap { user ->
                val updatedUser = user.copy(
                    firstName = updateRequest.firstName ?: user.firstName,
                    lastName = updateRequest.lastName ?: user.lastName,
                    bio = updateRequest.bio ?: user.bio,
                    socialLinks = updateRequest.socialLinks ?: user.socialLinks,
                    updatedAt = Instant.now()
                )
                userRepository.save(updatedUser)
            }
    }

    fun updateProfilePicture(userId: ObjectId, profilePictureUrl: String): Mono<User> {
        return findById(userId)
            .flatMap { user ->
                val updatedUser = user.copy(
                    profilePicture = profilePictureUrl,
                    updatedAt = Instant.now()
                )
                userRepository.save(updatedUser)
            }
    }

    fun followUser(followerId: ObjectId, followingId: ObjectId): Mono<Pair<User, User>> {
        if (followerId == followingId) {
            return Mono.error(IllegalArgumentException("Cannot follow yourself"))
        }

        return Mono.zip(
            findById(followerId),
            findById(followingId)
        ).flatMap { tuple ->
            val follower = tuple.t1
            val following = tuple.t2

            if (follower.following.contains(followingId)) {
                return@flatMap Mono.error<Pair<User, User>>(
                    IllegalStateException("Already following this user")
                )
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

            Mono.zip(
                userRepository.save(updatedFollower),
                userRepository.save(updatedFollowing)
            ).map { savedTuple ->
                Pair(savedTuple.t1, savedTuple.t2)
            }
        }
    }

    fun unfollowUser(followerId: ObjectId, followingId: ObjectId): Mono<Pair<User, User>> {
        if (followerId == followingId) {
            return Mono.error(IllegalArgumentException("Cannot unfollow yourself"))
        }

        return Mono.zip(
            findById(followerId),
            findById(followingId)
        ).flatMap { tuple ->
            val follower = tuple.t1
            val following = tuple.t2

            if (!follower.following.contains(followingId)) {
                return@flatMap Mono.error<Pair<User, User>>(
                    IllegalStateException("Not following this user")
                )
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

            Mono.zip(
                userRepository.save(updatedFollower),
                userRepository.save(updatedFollowing)
            ).map { savedTuple ->
                Pair(savedTuple.t1, savedTuple.t2)
            }
        }
    }

    fun toDTO(user: User): UserDTO {
        return UserDTO(
            id = user.id.toHexString(),
            username = user.username,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            fullName = "${user.firstName} ${user.lastName}",
            profilePicture = user.profilePicture,
            bio = user.bio,
            role = user.role,
            socialLinks = user.socialLinks,
            statistics = user.statistics,
            isEmailVerified = user.isEmailVerified,
            createdAt = user.createdAt,
            lastLoginAt = user.lastLoginAt
        )
    }
}