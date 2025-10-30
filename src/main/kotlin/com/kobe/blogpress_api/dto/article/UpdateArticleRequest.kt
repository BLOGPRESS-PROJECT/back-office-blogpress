package com.kobe.blogpress_api.dto.article

import jakarta.validation.constraints.Size
import java.time.Instant

data class UpdateArticleRequest(
    @field:Size(min = 3, max = 200, message = "Le titre doit contenir entre 3 et 200 caractères")
    val title: String? = null,

    @field:Size(min = 20, message = "Le contenu doit contenir au moins 20 caractères")
    val content: String? = null,

    @field:Size(max = 500, message = "Le résumé ne peut pas dépasser 500 caractères")
    val excerpt: String? = null,
    val coverImageUrl: String? = null,

    @field:Size(max = 10, message = "Maximum 10 tags autorisés")
    val tags: List<String>? = null,

    @field:Size(max = 50, message = "La catégorie ne peut pas dépasser 50 caractères")
    val category: String? = null,
    val isPublished: Boolean? = null,
    val isPrivate: Boolean? = null,
    val publishAt: Instant? = null
)