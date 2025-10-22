package com.kobe.blogpress_api.configuration.security.jwt

import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationManager(
    private val jwtService: JwtService
) : ReactiveAuthenticationManager {

    override fun authenticate(authentication: Authentication): Mono<Authentication> {
        return Mono.just(authentication)
            .map { it.credentials.toString() }
            .filter { jwtService.validateToken(it) && !jwtService.isTokenExpired(it) }
            .map { token ->
                val userId = jwtService.extractUserId(token)
                val role = jwtService.extractRole(token)
                val email = jwtService.extractEmail(token)

                val authorities = listOf(SimpleGrantedAuthority("ROLE_${role.name}"))

                UsernamePasswordAuthenticationToken(
                    userId.toHexString(),
                    token,
                    authorities
                )
            }
            .switchIfEmpty(Mono.error(RuntimeException("Invalid JWT token")))
    }
}