package com.kobe.blogpress_api.configuration

import com.kobe.blogpress_api.domain.model.user.Role
import com.kobe.blogpress_api.domain.model.user.User
import com.kobe.blogpress_api.repository.user.UserRepository
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class DataInitializer(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${admin.email}") private val adminEmail: String,
    @Value("\${admin.password}") private val adminPassword: String,
    @Value("\${admin.username}") private val adminUsername: String,
    @Value("\${admin.firstname}") private val adminFirstName: String,
    @Value("\${admin.lastname}") private val adminLastName: String,
) {

    private val logger = LoggerFactory.getLogger(DataInitializer::class.java)

    @PostConstruct
    fun init() {
        createAdminUser()
            .subscribe(
                { user ->
                    logger.info("✅ Admin user initialized: ${user.email}")
                },
                { error ->
                    logger.error("❌ Failed to initialize admin user: ${error.message}")
                }
            )
    }

    private fun createAdminUser(): Mono<User> {
        return userRepository.findByEmail(adminEmail)
            .switchIfEmpty(
                Mono.defer {
                    logger.info("Creating default admin user...")
                    val adminUser = User(
                        username = adminUsername,
                        email = adminEmail,
                        password = passwordEncoder.encode(adminPassword),
                        firstName = adminFirstName,
                        lastName = adminLastName,
                        role = Role.ADMIN,
                        isEmailVerified = true,
                        bio = "Default system administrator"
                    )
                    userRepository.save(adminUser)
                }
            )
            .doOnSuccess { user ->
                if (user != null) {
                    logger.info("Admin user exists: ${user.email}")
                }
            }
    }
}