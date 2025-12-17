package com.kobe.blogpress_api.dto.article

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotEmpty
import java.time.Instant

data class BatchCreateBlogPostsRequestDTO(
    @field:NotEmpty(message = "Blog IDs list cannot be empty")
    val blogIds: List<String>, // Liste des IDs de blogs pour lesquels créer des articles
    
    @field:Min(1, message = "Posts per blog must be at least 1")
    @field:Max(50, message = "Posts per blog cannot exceed 50")
    val postsPerBlog: Int = 5, // Nombre d'articles à créer par blog
    
    val titlePrefix: String = "Article de blog", // Le titre sera "{titlePrefix} {index}"
    val publishSome: Boolean = true, // Si true, certains articles seront publiés
    val publishedPercentage: Int = 70, // Pourcentage d'articles publiés (si publishSome = true)
    val makeSomePrivate: Boolean = false, // Si true, certains articles seront privés
    val privatePercentage: Int = 10, // Pourcentage d'articles privés (si makeSomePrivate = true)
    val scheduleSome: Boolean = false, // Si true, certains articles auront une date de publication programmée
    val scheduledPercentage: Int = 20 // Pourcentage d'articles programmés (si scheduleSome = true)
)

