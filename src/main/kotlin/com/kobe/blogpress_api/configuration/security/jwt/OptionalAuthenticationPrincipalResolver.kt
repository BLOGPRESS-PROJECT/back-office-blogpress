package com.kobe.blogpress_api.configuration.security.jwt

import org.springframework.core.MethodParameter
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.web.reactive.BindingContext
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Resolver personnalisé pour gérer @AuthenticationPrincipal de manière optionnelle
 * sur les routes publiques où l'authentification n'est pas requise
 */
class OptionalAuthenticationPrincipalResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        val hasAnnotation = parameter.hasParameterAnnotation(org.springframework.security.core.annotation.AuthenticationPrincipal::class.java)
        val isStringType = parameter.parameterType == String::class.java || 
                          parameter.parameterType == String::class.javaObjectType
        
        // Vérifier si le paramètre est nullable en Kotlin (String?)
        // En Kotlin, un paramètre nullable est représenté par String::class.javaObjectType
        val isNullable = parameter.parameterType == String::class.javaObjectType
        
        return hasAnnotation && isStringType && isNullable
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        bindingContext: BindingContext,
        exchange: ServerWebExchange
    ): Mono<Any> {
        return ReactiveSecurityContextHolder.getContext()
            .cast(SecurityContext::class.java)
            .mapNotNull { it.authentication }
            .filter { it.isAuthenticated }
            .mapNotNull { authentication: Authentication ->
                authentication.principal as? String
            }
            .cast(Any::class.java)
            .defaultIfEmpty("" as Any)
    }
}

