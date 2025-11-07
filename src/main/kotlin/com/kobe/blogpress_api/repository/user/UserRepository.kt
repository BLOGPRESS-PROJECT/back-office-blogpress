package com.kobe.blogpress_api.repository.user

import com.kobe.blogpress_api.domain.model.user.Role
import com.kobe.blogpress_api.domain.model.user.User
import org.bson.types.ObjectId
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface UserRepository : ReactiveMongoRepository<User, ObjectId> {
    fun findByEmail(email: String): Mono<User>
    fun findByUsername(username: String): Mono<User>
    fun existsByEmail(email: String): Mono<Boolean>
    fun existsByUsername(username: String): Mono<Boolean>
    fun findByUsernameOrEmailOrFullName(regex: kotlin.text.Regex, of: org.springframework.data.domain.PageRequest): org.springframework.data.domain.Page<com.kobe.blogpress_api.domain.model.user.User>
}