package com.kobe.blogpress_api.configuration.security

import com.kobe.blogpress_api.configuration.security.jwt.JwtAuthenticationManager
import com.kobe.blogpress_api.configuration.security.jwt.JwtServerAuthenticationConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationManager: JwtAuthenticationManager,
    private val jwtServerAuthenticationConverter: JwtServerAuthenticationConverter
) {

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        val authenticationWebFilter = AuthenticationWebFilter(jwtAuthenticationManager).apply {
            setServerAuthenticationConverter(jwtServerAuthenticationConverter)
        }

        return http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .authorizeExchange { exchanges ->
                exchanges
                    // ===== ROUTES PUBLIQUES =====
                    // Authentification
                    .pathMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh").permitAll()

                    // Lecture publique des blogs et articles
                    .pathMatchers(HttpMethod.GET, "/api/blogs", "/api/blogs/**").permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/articles", "/api/articles/**").permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/posts", "/api/posts/**").permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/users/profile/**").permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/users/username/**").permitAll()
                    // Recherche publique
                    .pathMatchers(HttpMethod.GET, "/api/search", "/api/search/**").permitAll()

                    // Interactions publiques (views, shares - pas besoin d'auth)
                    .pathMatchers(HttpMethod.POST, "/api/content/*/view").permitAll()
                    .pathMatchers(HttpMethod.POST, "/api/content/*/share").permitAll()

                    // Upload d'images (public pour le moment, on sécurisera plus tard)
                    .pathMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                    // Upload d'images (authentifié)
                    .pathMatchers(HttpMethod.POST, "/api/blogs/*/cover-image").authenticated()
                    .pathMatchers(HttpMethod.POST, "/api/blogs/*/logo-image").authenticated()
                    .pathMatchers(HttpMethod.DELETE, "/api/blogs/*/cover-image").authenticated()
                    .pathMatchers(HttpMethod.DELETE, "/api/blogs/*/logo-image").authenticated()
                    .pathMatchers(HttpMethod.POST, "/api/articles/*/cover-image").authenticated()
                    .pathMatchers(HttpMethod.DELETE, "/api/articles/*/cover-image").authenticated()

                    // Documentation et health
                    .pathMatchers("/actuator/health", "/health").permitAll()

                    // ===== ROUTES ADMIN =====
                    .pathMatchers("/api/admin/**").hasRole("ADMIN")

                    // Golden User management (ADMIN only)
                    .pathMatchers("/api/users/*/promote-golden").hasRole("ADMIN")
                    .pathMatchers("/api/users/*/revoke-golden").hasRole("ADMIN")

                    // ===== ROUTES UTILISATEUR AUTHENTIFIÉ =====
                    // User management
                    .pathMatchers("/api/users/me/**").authenticated()
                    .pathMatchers(HttpMethod.PUT, "/api/users/**").authenticated()
                    .pathMatchers(HttpMethod.DELETE, "/api/users/**").authenticated()
                    .pathMatchers("/api/users/follow/**").authenticated()
                    .pathMatchers("/api/users/unfollow/**").authenticated()

                    // Blog management (création, modification, suppression)
                    .pathMatchers(HttpMethod.POST, "/api/blogs").authenticated()
                    .pathMatchers(HttpMethod.PUT, "/api/blogs/**").authenticated()
                    .pathMatchers(HttpMethod.DELETE, "/api/blogs/**").authenticated()
                    .pathMatchers(HttpMethod.GET, "/api/blogs/user").authenticated()

                    // Article management (création, modification, suppression)
                    .pathMatchers(HttpMethod.POST, "/api/articles").authenticated()
                    .pathMatchers(HttpMethod.POST, "/api/blogs/*/posts").authenticated()
                    .pathMatchers(HttpMethod.PUT, "/api/articles/**").authenticated()
                    .pathMatchers(HttpMethod.DELETE, "/api/articles/**").authenticated()
                    .pathMatchers(HttpMethod.GET, "/api/articles/user").authenticated()


                    // Interactions authentifiées (likes, favorites)
                    .pathMatchers(HttpMethod.POST, "/api/content/*/like").authenticated()
                    .pathMatchers(HttpMethod.DELETE, "/api/content/*/like").authenticated()
                    .pathMatchers(HttpMethod.POST, "/api/content/*/favorite").authenticated()
                    .pathMatchers(HttpMethod.DELETE, "/api/content/*/favorite").authenticated()

                    // Toutes les autres routes nécessitent une authentification
                    .anyExchange().authenticated()
            }
            .addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOriginPatterns = listOf("*")
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
            allowedHeaders = listOf("*")
            exposedHeaders = listOf("Authorization", "X-Total-Count")
            allowCredentials = true
            maxAge = 3600
        }

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}

/*
* bon je veux une architechture un peu comme ca :
* creer un directory pour blogpress-db, blogpress-api
*
* setup/blogpress-db/-docker-compose-db.yaml, -env-db
* setup/blogpress-api/ dokcer-compose-api.yaml, -env-api
*
*
*
*
*
* */