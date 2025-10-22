package com.kobe.blogpress_api.configuration.security.jwt

import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class JwtServerAuthenticationConverter : ServerAuthenticationConverter {

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun convert(exchange: ServerWebExchange): Mono<Authentication> {
        return Mono.justOrEmpty(exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION))
            .filter { it.startsWith(BEARER_PREFIX) }
            .map { it.substring(BEARER_PREFIX.length) }
            .map { UsernamePasswordAuthenticationToken(it, it) }
    }
}