package com.kobe.blogpress_api.dto.feed

/**
 * Réponse paginée pour le feed.
 */
data class FeedResponse(
    val content: List<FeedItemDto>, // Liste des articles
    val page: Int, // Page actuelle (0-based)
    val size: Int, // Taille de la page
    val totalElements: Long, // Nombre total d'éléments
    val totalPages: Int, // Nombre total de pages
    val hasNext: Boolean, // Y a-t-il une page suivante ?
    val hasPrevious: Boolean, // Y a-t-il une page précédente ?
    val isFirst: Boolean, // Est-ce la première page ?
    val isLast: Boolean // Est-ce la dernière page ?
)



