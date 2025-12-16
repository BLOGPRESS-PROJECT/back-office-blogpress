package com.kobe.blogpress_api.dto.article

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Max
import java.time.Instant

data class BatchCreateArticlesRequestDTO(
    @field:Min(1, message = "Count must be at least 1")
    @field:Max(100, message = "Count cannot exceed 100")
    val count: Int = 10,
    
    val titlePrefix: String = "Article de test", // Le titre sera "{titlePrefix} {index}"
    val publishSome: Boolean = true, // Si true, certains articles seront publiés
    val publishedPercentage: Int = 70, // Pourcentage d'articles publiés (si publishSome = true)
    val makeSomePrivate: Boolean = false, // Si true, certains articles seront privés
    val privatePercentage: Int = 10, // Pourcentage d'articles privés (si makeSomePrivate = true)
    val scheduleSome: Boolean = false, // Si true, certains articles auront une date de publication programmée
    val scheduledPercentage: Int = 20 // Pourcentage d'articles programmés (si scheduleSome = true)
)

