package com.kobe.blogpress_api.domain.dto.blog

import jakarta.validation.constraints.Size
import java.time.Instant

data class UpdateBlogRequest(
    @field:Size(min = 3, max = 100, message = "Le titre doit contenir entre 3 et 100 caractères")
    val title: String? = null,

    @field:Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
    val description: String? = null,

    @field:Size(max = 10, message = "Maximum 10 tags autorisés")
    val tags: List<String>? = null,

    val logoImageUrl: String? = null,

    val coverImageUrl: String? = null,

    val isPublished: Boolean? = null,

    val isPrivate: Boolean? = null,

    val publishAt: Instant? = null
)