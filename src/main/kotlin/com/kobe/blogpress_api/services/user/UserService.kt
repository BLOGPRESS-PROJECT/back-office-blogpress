package com.kobe.blogpress_api.services.user

import com.kobe.blogpress_api.domain.model.user.Gender
import com.kobe.blogpress_api.domain.model.user.Role
import com.kobe.blogpress_api.domain.model.user.SocialLinks
import com.kobe.blogpress_api.domain.model.user.User
import com.kobe.blogpress_api.dto.user.BatchCreateUsersRequestDTO
import com.kobe.blogpress_api.dto.user.PrivacyPreferencesDTO
import com.kobe.blogpress_api.dto.user.UpdateProfileRequestDTO
import com.kobe.blogpress_api.dto.user.UserDTO
import com.kobe.blogpress_api.exception.ResourceNotFoundException
import com.kobe.blogpress_api.repository.blog.BlogRepository
import com.kobe.blogpress_api.repository.article.ArticleRepository
import com.kobe.blogpress_api.repository.user.UserRepository
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.util.Random

@Service
class UserService(
    private val userRepository: UserRepository,
    private val blogRepository: BlogRepository,
    private val articleRepository: ArticleRepository,
    private val mongoTemplate: ReactiveMongoTemplate,
    private val passwordEncoder: PasswordEncoder
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(UserService::class.java)

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

        val savedUser = userRepository.save(updatedUser).awaitSingle()

        // ⭐ Mettre à jour le quota de stockage pour qu'il soit illimité
        // Note: Cette logique est gérée dans AdminUserController pour éviter les dépendances circulaires

        return savedUser
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
        val pageable = PageRequest.of(page, size)
        
        // Construire la query avec recherche
        val searchQuery = Query().apply {
            addCriteria(
                Criteria().orOperator(
                    Criteria.where("username").regex(query, "i"),
                    Criteria.where("email").regex(query, "i"),
                    Criteria.where("fullName").regex(query, "i")
                )
            )
        }
        
        // Query paginée pour les données
        val dataQuery = Query.of(searchQuery).with(pageable)
        val users = mongoTemplate.find(dataQuery, User::class.java)
            .collectList()
            .awaitSingle()
        
        // Query sans pagination pour le total
        val countQuery = Query.of(searchQuery).limit(-1).skip(-1)
        val total = mongoTemplate.count(countQuery, User::class.java).awaitSingle()
        
        return PageImpl(users, pageable, total)
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
     * Récupérer tous les utilisateurs paginés (pour l'admin) avec filtres.
     */
    suspend fun findAllUsers(
        page: Int,
        size: Int,
        search: String? = null,
        role: String? = null,
        isGolden: Boolean? = null,
        isActive: Boolean? = null
    ): Page<User> {
        val pageable = PageRequest.of(page, size)

        // Construire la query de base
        val baseQuery = Query()

        if (!search.isNullOrBlank()) {
            baseQuery.addCriteria(
                Criteria().orOperator(
                    Criteria.where("username").regex(search, "i"),
                    Criteria.where("email").regex(search, "i"),
                    Criteria.where("fullName").regex(search, "i")
                )
            )
        }

        if (!role.isNullOrBlank()) {
            val roleEnum = Role.valueOf(role)
            baseQuery.addCriteria(Criteria.where("role").`is`(roleEnum))
        }

        if (isGolden != null) {
            baseQuery.addCriteria(Criteria.where("isGoldenUser").`is`(isGolden))
        }

        if (isActive != null) {
            baseQuery.addCriteria(Criteria.where("isActive").`is`(isActive))
        }

        // Query paginée pour les données
        val dataQuery = Query.of(baseQuery).with(pageable)
        val users = mongoTemplate.find(dataQuery, User::class.java)
            .collectList()
            .awaitSingle()

        // Query sans pagination pour le total
        val countQuery = Query.of(baseQuery).limit(-1).skip(-1)
        val total = mongoTemplate.count(countQuery, User::class.java).awaitSingle()

        return PageImpl(users, pageable, total)
    }

    /**
     * Désactiver un utilisateur (ADMIN seulement via routes /api/admin).
     */
    suspend fun deactivateUser(userId: ObjectId): User {
        val user = findById(userId)
        val updated = user.copy(isActive = false, updatedAt = Instant.now())
        return userRepository.save(updated).awaitSingle()
    }

    /**
     * Activer un utilisateur (ADMIN seulement via routes /api/admin).
     */
    suspend fun activateUser(userId: ObjectId): User {
        val user = findById(userId)
        val updated = user.copy(isActive = true, updatedAt = Instant.now())
        return userRepository.save(updated).awaitSingle()
    }

    /**
     * Supprimer un utilisateur (ADMIN).
     * TODO: gérer la suppression/anonymisation des contenus associés si nécessaire.
     */
    suspend fun deleteUser(userId: ObjectId) {
        val user = findById(userId)
        userRepository.delete(user).awaitSingleOrNull()
    }

    /**
     * Convertit un User en UserDTO avec statistiques calculées à la volée.
     */
    suspend fun toDTO(user: User): UserDTO {
        // Calculer les statistiques à la volée pour s'assurer qu'elles sont à jour
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
            statistics = updatedStatistics,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
            lastLoginAt = user.lastLoginAt
        )
    }

    /**
     * Mapping léger pour la liste admin des utilisateurs.
     */
    suspend fun toAdminListItemDTO(user: User): com.kobe.blogpress_api.dto.user.AdminUserListItemDTO {
        val stats = calculateUserStatistics(user.id)
        return com.kobe.blogpress_api.dto.user.AdminUserListItemDTO(
            id = user.id.toHexString(),
            username = user.username,
            email = user.email,
            fullName = "${user.firstName} ${user.lastName}",
            role = user.role,
            isActive = user.isActive,
            isGoldenUser = user.isGoldenUser,
            goldenUserSince = user.goldenUserSince,
            createdAt = user.createdAt,
            lastLoginAt = user.lastLoginAt,
            statistics = stats
        )
    }

    /**
     * Calcule l'age a partir de la date de naissance.
     */
    private fun calculateAge(birthDate: LocalDate): Int {
        return java.time.Period.between(birthDate, LocalDate.now()).years
    }

    /**
     * Créer plusieurs utilisateurs en batch pour les tests.
     */
    suspend fun batchCreateUsers(request: BatchCreateUsersRequestDTO): Map<String, Any> = coroutineScope {
        val random = Random()
        val firstNames = listOf(
            "Alexandre", "Marie", "Thomas", "Sophie", "Lucas", "Emma", "Hugo", "Léa",
            "Louis", "Chloé", "Antoine", "Camille", "Pierre", "Julie", "Nicolas", "Sarah",
            "Maxime", "Laura", "Julien", "Manon", "Paul", "Clara", "Baptiste", "Inès",
            "Romain", "Élise", "Vincent", "Anaïs", "Matthieu", "Marion", "Benjamin", "Lucie"
        )
        val lastNames = listOf(
            "Martin", "Bernard", "Dubois", "Thomas", "Robert", "Richard", "Petit", "Durand",
            "Leroy", "Moreau", "Simon", "Laurent", "Lefebvre", "Michel", "Garcia", "David",
            "Bertrand", "Roux", "Vincent", "Fournier", "Morel", "Girard", "André", "Lefevre",
            "Mercier", "Dupont", "Lambert", "Bonnet", "François", "Martinez", "Legrand", "Garnier"
        )
        val countries = listOf("France", "Belgique", "Suisse", "Canada", "Maroc", "Sénégal", "Côte d'Ivoire", "Algérie")
        val interestsList = listOf(
            listOf("Technologie", "Programmation"),
            listOf("Voyage", "Photographie"),
            listOf("Musique", "Cinéma"),
            listOf("Sport", "Fitness"),
            listOf("Lecture", "Écriture"),
            listOf("Cuisine", "Gastronomie"),
            listOf("Art", "Design"),
            listOf("Science", "Recherche")
        )

        val timestamp = System.currentTimeMillis()
        val createdUsers = mutableListOf<String>()
        var goldenCount = 0

        val users = (1..request.count).map { index ->
            async {
                val firstName = firstNames[random.nextInt(firstNames.size)]
                val lastName = lastNames[random.nextInt(lastNames.size)]
                val username = "testuser${timestamp}_$index"
                val email = "testuser${timestamp}_$index@example.com"
                val password = "${request.passwordPrefix}${index}!"

                // Déterminer si l'utilisateur sera Golden
                val isGolden = if (request.makeSomeGolden) {
                    (random.nextInt(100) < request.goldenPercentage)
                } else {
                    false
                }

                val user = User(
                    username = username.lowercase(),
                    email = email.lowercase(),
                    password = passwordEncoder.encode(password),
                    firstName = firstName,
                    lastName = lastName,
                    birthDate = LocalDate.of(
                        1990 + random.nextInt(30),
                        1 + random.nextInt(12),
                        1 + random.nextInt(28)
                    ),
                    gender = Gender.values()[random.nextInt(Gender.values().size)],
                    country = countries[random.nextInt(countries.size)],
                    phoneNumber = "+336${String.format("%08d", random.nextInt(100000000))}",
                    interests = interestsList[random.nextInt(interestsList.size)],
                    preferredLanguage = listOf("fr", "en", "es")[random.nextInt(3)],
                    bio = "Utilisateur de test créé automatiquement - Index: $index",
                    role = Role.USER,
                    isGoldenUser = isGolden,
                    goldenUserSince = if (isGolden) Instant.now() else null
                )

                try {
                    val savedUser = userRepository.save(user).awaitSingle()
                    createdUsers.add(savedUser.id.toHexString())
                    if (isGolden) goldenCount++
                    logger.info("Created test user: ${savedUser.username} (Golden: $isGolden)")
                    savedUser
                } catch (e: Exception) {
                    logger.warn("Failed to create user $username: ${e.message}")
                    null
                }
            }
        }.awaitAll().filterNotNull()

        mapOf(
            "totalRequested" to request.count,
            "totalCreated" to users.size,
            "goldenUsersCreated" to goldenCount,
            "createdUserIds" to createdUsers,
            "message" to "Batch user creation completed",
        )
    }
}