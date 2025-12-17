package com.kobe.blogpress_api.dto.blog

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Max
import java.time.Instant

data class BatchCreateBlogsRequestDTO(
    @field:Min(1, message = "Count must be at least 1")
    @field:Max(100, message = "Count cannot exceed 100")
    val count: Int = 10,
    
    val titlePrefix: String = "Blog de test", // Le titre sera "{titlePrefix} {index}"
    val publishSome: Boolean = true, // Si true, certains blogs seront publiés
    val publishedPercentage: Int = 60, // Pourcentage de blogs publiés (si publishSome = true)
    val makeSomePrivate: Boolean = false, // Si true, certains blogs seront privés
    val privatePercentage: Int = 15, // Pourcentage de blogs privés (si makeSomePrivate = true)
    val scheduleSome: Boolean = false, // Si true, certains blogs auront une date de publication programmée
    val scheduledPercentage: Int = 20 // Pourcentage de blogs programmés (si scheduleSome = true)
)

