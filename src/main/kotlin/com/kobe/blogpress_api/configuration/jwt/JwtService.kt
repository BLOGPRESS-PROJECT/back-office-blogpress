package com.kobe.blogpress_api.configuration.jwt

import com.kobe.blogpress_api.model.user.RolePermissionConfig
import com.kobe.blogpress_api.model.user.RoleType
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.Base64
import java.util.Date

@Service
class JwtService(
    @Value("\${jwt.secret}") private val jwtSecret: String,
) {
    private val secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret))
    private val accessTokenValidityMs = 60L * 60L * 1000L // 1 heure et 00 minutes
    val refreshTokenValidityMs = 7L * 24 * 60 * 60 * 1000L // 7 jours heures

    private fun generateToken(
        userId: String,
        type: String,
        roleType: RoleType? = null,
        expiry: Long
    ): String {
        val now = Date()
        val expiryDate = Date(now.time + expiry)

        val builder = Jwts.builder()
            .subject(userId)
            .claim("type", type)
            .issuedAt(now)
            .expiration(expiryDate)

        if (roleType != null) {
            builder.claim("role", roleType.name)
            builder.claim("department", roleType.department)
            builder.claim("permissions", RolePermissionConfig.getPermissions(roleType).map { it.name })
        }

        return builder
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact()
    }

    fun generateAccessToken(userId: String, roleType: RoleType): String {
        return generateToken(userId, "access", roleType, accessTokenValidityMs)
    }

    fun generateRefreshToken(userId: String): String {
        return generateToken(userId, "refresh", null, refreshTokenValidityMs)
    }

    fun validateAccessToken(token: String): Boolean {
        val claims = parseAllClaims(token) ?: return false
        val tokenType = claims["type"] as? String ?: return false
        return tokenType == "access"
    }

    fun validateRefreshToken(token: String): Boolean {
        val claims = parseAllClaims(token) ?: return false
        val tokenType = claims["type"] as? String ?: return false
        return tokenType == "refresh"
    }

    fun getUserIdFromToken(token: String): String {
        val claims = parseAllClaims(token) ?: throw ResponseStatusException(
            HttpStatusCode.valueOf(401),
            "Token invalide."
        )
        return claims.subject
    }

    fun getRoleFromToken(token: String): RoleType? {
        val claims = parseAllClaims(token) ?: return null
        val roleName = claims["role"] as? String ?: return null
        return try {
            RoleType.valueOf(roleName)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun parseAllClaims(token: String): Claims? {
        val rawToken = if(token.startsWith("Bearer ")) {
            token.removePrefix("Bearer ")
        } else token
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(rawToken)
                .payload
        } catch(e: Exception) {
            null
        }
    }
}