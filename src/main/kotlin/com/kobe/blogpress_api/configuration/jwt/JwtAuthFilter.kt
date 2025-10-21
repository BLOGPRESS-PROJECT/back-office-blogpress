package com.kobe.blogpress_api.configuration.jwt

import com.kobe.blogpress_api.model.user.RolePermissionConfig
import com.kobe.blogpress_api.repository.user.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.bson.types.ObjectId
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import kotlin.text.startsWith

@Component
class JwtAuthFilter(
    private val jwtService: JwtService,
    private val userRepository: UserRepository
): OncePerRequestFilter() {
    //
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            if (jwtService.validateAccessToken(authHeader)) {
                val userId = jwtService.getUserIdFromToken(authHeader)
                val roleType = jwtService.getRoleFromToken(authHeader)

                // Vérifier que l'utilisateur existe et est actif
                val user = userRepository.findById(ObjectId(userId)).orElse(null)

                if (user != null && user.isActive) {
                    val authorities = mutableListOf<GrantedAuthority>()

                    // Ajouter le rôle principal
                    authorities.add(SimpleGrantedAuthority("ROLE_${roleType?.name}"))
                    //authorities.add(SimpleGrantedAuthority("${roleType?.name}"))

                    // Ajouter les permissions comme autorités
                    if (roleType != null) {
                        RolePermissionConfig.getPermissions(roleType).forEach { permission ->
                            authorities.add(SimpleGrantedAuthority("${permission.name}"))
                            //authorities.add(SimpleGrantedAuthority("PERMISSION_${permission.name}"))
                        }
                    }

                    val auth = UsernamePasswordAuthenticationToken(userId, null, authorities)
                    SecurityContextHolder.getContext().authentication = auth

                    // ✅ AJOUT IMPORTANT : Définir l'attribut currentUserId dans la requête
                    request.setAttribute("currentUserId", userId)
                }
            }
        }
        filterChain.doFilter(request, response)
    }
}