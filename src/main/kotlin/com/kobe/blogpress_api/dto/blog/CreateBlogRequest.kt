package com.kobe.blogpress_api.dto.blog

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class CreateBlogRequest(
    @field:NotBlank(message = "Le titre du blog est obligatoire")
    @field:Size(min = 3, max = 100, message = "Le titre doit contenir entre 3 et 100 caractères")
    val title: String,
    @field:Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
    val description: String? = null,
    @field:Size(max = 10, message = "Maximum 10 tags autorisés")
    val tags: List<String>? = null,
    val logoImageUrl: String? = null,
    val coverImageUrl: String? = null,
    val isPublished: Boolean = false,
    val isPrivate: Boolean = false,
    val publishAt: Instant? = null
)