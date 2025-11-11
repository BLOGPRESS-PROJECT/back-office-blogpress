package com.kobe.blogpress_api.dto.user

data class SocialLinksDTO(
    val twitter: String? = null,
    val linkedin: String? = null,
    val github: String? = null,
    val facebook: String? = null,
    val instagram: String? = null
)