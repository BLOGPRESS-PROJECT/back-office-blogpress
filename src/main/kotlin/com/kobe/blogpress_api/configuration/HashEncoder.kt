package com.kobe.blogpress_api.configuration

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class HashEncoder {
    private val bCrypt = BCryptPasswordEncoder()

    fun encode(password: String): String {
        return bCrypt.encode(password)
    }

    fun matches(rawPassword: String, encodedPassword: String): Boolean {
        return bCrypt.matches(rawPassword, encodedPassword)
    }
}