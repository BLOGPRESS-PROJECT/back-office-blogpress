package com.kobe.blogpress_api.dto.user

import com.kobe.blogpress_api.domain.model.user.Gender
import com.kobe.blogpress_api.domain.model.user.SocialLinks
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class UpdateProfileRequestDTO(
    @field:Size(min = 2, max = 50)
    val firstName: String? = null,

    @field:Size(min = 2, max = 50)
    val lastName: String? = null,

    @field:Past(message = "Birth date must be in the past")
    val birthDate: LocalDate? = null,

    val gender: Gender? = null,

    @field:Size(max = 100)
    val country: String? = null,

    @field:Pattern(regexp = "^[+]?[0-9]{10,15}$")
    val phoneNumber: String? = null,

    @field:Size(max = 10)
    val interests: List<String>? = null,

    @field:Pattern(regexp = "^(fr|en|es|de|it|pt)$")
    val preferredLanguage: String? = null,

    @field:Size(max = 500)
    val bio: String? = null,

    @field:Size(max = 200)
    val website: String? = null,

    val socialLinks: SocialLinks? = null
)