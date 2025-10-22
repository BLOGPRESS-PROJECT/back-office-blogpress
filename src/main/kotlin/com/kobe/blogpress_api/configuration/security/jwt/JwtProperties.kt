package com.kobe.blogpress_api.configuration.security.jwt

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    var secret: String = "",
    var accessTokenExpiration: Long = 3600000, // 1 heure
    var refreshTokenExpiration: Long = 604800000 // 7 jours
)