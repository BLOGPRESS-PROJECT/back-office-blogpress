package com.kobe.blogpress_api.configuration.security.jwt

import com.kobe.blogpress_api.domain.model.user.Role
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Base64
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    private val jwtProperties: JwtProperties
) {
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(
        Base64.getDecoder().decode(jwtProperties.secret)
    )

    fun generateAccessToken(userId: ObjectId, email: String, role: Role): String {
        val now = Instant.now()
        val expiryDate = Date.from(now.plusMillis(jwtProperties.accessTokenExpiration))

        return Jwts.builder()
            .subject(userId.toHexString())
            .claim("email", email)
            .claim("role", role.name)
            .claim("type", "ACCESS")
            .issuedAt(Date.from(now))
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    fun generateRefreshToken(userId: ObjectId, rememberMe: Boolean = false): String {
        val now = Instant.now()
        val expirationMillis = if (rememberMe) {
            jwtProperties.rememberMeRefreshTokenExpiration
        } else {
            jwtProperties.refreshTokenExpiration
        }
        val expiryDate = Date.from(now.plusMillis(expirationMillis))

        return Jwts.builder()
            .subject(userId.toHexString())
            .claim("type", "REFRESH")
            .claim("rememberMe", rememberMe)
            .issuedAt(Date.from(now))
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun extractUserId(token: String): ObjectId {
        val claims = extractAllClaims(token)
        return ObjectId(claims.subject)
    }

    fun extractEmail(token: String): String {
        return extractAllClaims(token)["email"] as String
    }

    fun extractRole(token: String): Role {
        val roleName = extractAllClaims(token)["role"] as String
        return Role.valueOf(roleName)
    }

    fun extractTokenType(token: String): String {
        return extractAllClaims(token)["type"] as String
    }

    fun isTokenExpired(token: String): Boolean {
        return try {
            val expiration = extractAllClaims(token).expiration
            expiration.before(Date())
        } catch (e: Exception) {
            true
        }
    }

    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    /**
     * Méthode interne exposée pour les services qui ont besoin d'accéder aux claims bruts.
     * À utiliser avec précaution (ex: pour relire le flag rememberMe dans AuthService.refreshToken).
     */
    internal fun extractAllClaimsInternal(token: String): Claims = extractAllClaims(token)

    fun getAccessTokenExpiration(): Long = jwtProperties.accessTokenExpiration
}