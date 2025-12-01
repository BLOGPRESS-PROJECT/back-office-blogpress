package com.kobe.blogpress_api.configuration.security

import com.kobe.blogpress_api.configuration.security.jwt.JwtAuthenticationManager
import com.kobe.blogpress_api.configuration.security.jwt.JwtServerAuthenticationConverter
import com.kobe.blogpress_api.configuration.security.jwt.JwtServerSecurityContextRepository
import org.springframework.beans.factory.annotation.Value
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
import org.springframework.security.web.server.context.ServerSecurityContextRepository
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationManager: JwtAuthenticationManager,
    private val jwtServerAuthenticationConverter: JwtServerAuthenticationConverter,
    private val jwtServerSecurityContextRepository: JwtServerSecurityContextRepository,

    // ✅ Injecter les URLs configurables
    @Value("\${app.frontend-url}") private val frontendUrl: String,
    @Value("\${app.allowed-origins:http://localhost:3000}") private val allowedOrigins: String
) {

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        val authenticationWebFilter = AuthenticationWebFilter(jwtAuthenticationManager).apply {
            setServerAuthenticationConverter(jwtServerAuthenticationConverter)
            setSecurityContextRepository(jwtServerSecurityContextRepository)
        }

        return http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .securityContextRepository(jwtServerSecurityContextRepository)
            .authorizeExchange { exchanges ->
                exchanges
                    // ===== ROUTES PUBLIQUES =====
                    // Authentification
                    .pathMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh").permitAll()

                    // Lecture publique des blogs et articles
                    // Routes spécifiques pour les slugs et shareId (doivent être avant les routes générales)
                    .pathMatchers(HttpMethod.GET, "/api/blogs/slug/**").permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/blogs/share/**").permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/articles/share/**").permitAll() // ⭐ NOUVEAU : Articles par shareId
                    .pathMatchers(HttpMethod.GET, "/api/blogs", "/api/blogs/**").permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/articles", "/api/articles/**").permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/posts", "/api/posts/**").permitAll()
                    // Profils utilisateurs publics
                    .pathMatchers(HttpMethod.GET, "/api/users/profile/**").permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/users/username/**").permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/users/*/public").permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/users/{userId}").permitAll() // ⭐ NOUVEAU : Profil public par ID

                    // Profile Picture ROUTES PUBLIQUES
                    .pathMatchers(HttpMethod.GET, "/api/users/*/profile-picture").permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/users/profile-pictures/**").permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/users/*/profile-picture/metadata").permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/users/*/profile-picture/exists").permitAll()

                    // Blog Images ROUTES PUBLIQUES (cover et logo)
                    .pathMatchers(HttpMethod.GET, "/api/blogs/*/cover-image").permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/blogs/*/logo-image").permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/blogs/cover-images/**").permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/blogs/logo-images/**").permitAll()
                    
                    // Article Images ROUTES PUBLIQUES (cover)
                    .pathMatchers(HttpMethod.GET, "/api/articles/*/cover-image").permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/articles/cover-images/**").permitAll()


                    // Recherche publique
                    .pathMatchers(HttpMethod.GET, "/api/search", "/api/search/**").permitAll()

                    // Feed public (authentification optionnelle)
                    .pathMatchers(HttpMethod.GET, "/api/feed", "/api/feed/**").permitAll()

                    // Interactions publiques (views, shares)
                    // ⭐ IMPORTANT : Routes pour /api/interactions/view et /api/interactions/share
                    .pathMatchers(HttpMethod.POST, "/api/interactions/view").permitAll()
                    .pathMatchers(HttpMethod.POST, "/api/interactions/share").permitAll()
                    // Anciennes routes (pour compatibilité si elles existent encore)
                    .pathMatchers(HttpMethod.POST, "/api/content/*/view").permitAll()
                    .pathMatchers(HttpMethod.POST, "/api/content/*/share").permitAll()

                    // Upload d'images (public pour lecture)
                    .pathMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                    // Upload d'images (authentifié pour écriture)
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
                    .pathMatchers("/api/users/me", "/api/users/me/**").authenticated()
                    .pathMatchers(HttpMethod.PUT, "/api/users/**").authenticated()
                    .pathMatchers(HttpMethod.DELETE, "/api/users/**").authenticated()
                    .pathMatchers("/api/users/follow/**").authenticated()
                    .pathMatchers("/api/users/unfollow/**").authenticated()

                    // Blog management
                    .pathMatchers(HttpMethod.POST, "/api/blogs").authenticated()
                    .pathMatchers(HttpMethod.PUT, "/api/blogs/**").authenticated()
                    .pathMatchers(HttpMethod.DELETE, "/api/blogs/**").authenticated()
                    .pathMatchers(HttpMethod.GET, "/api/blogs/user").authenticated()

                    // Article management
                    .pathMatchers(HttpMethod.POST, "/api/articles").authenticated()
                    .pathMatchers(HttpMethod.POST, "/api/blogs/*/posts").authenticated()
                    .pathMatchers(HttpMethod.PUT, "/api/articles/**").authenticated()
                    .pathMatchers(HttpMethod.DELETE, "/api/articles/**").authenticated()
                    .pathMatchers(HttpMethod.GET, "/api/articles/user").authenticated()

                    // Interactions authentifiées
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
        // ✅ Parser les origins depuis la propriété (séparés par des virgules)
        val origins = allowedOrigins.split(",").map { it.trim() }

        println("🌐 CORS - Allowed Origins: $origins")
        println("🌐 CORS - Frontend URL: $frontendUrl")

        val configuration = CorsConfiguration().apply {
            // ✅ Utiliser les origins configurés
            allowedOrigins = origins

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