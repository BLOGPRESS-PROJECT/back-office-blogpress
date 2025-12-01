package com.kobe.blogpress_api.dto.feed

import com.kobe.blogpress_api.domain.model.article.ArticleType
import java.time.LocalDateTime

/**
 * DTO pour un article dans le feed principal.
 * Contient toutes les informations nécessaires pour l'affichage dans FeedItemCard.
 */
data class FeedItemDto(
    // Identifiants
    val id: String, // ObjectId converti en String
    val blogId: String?, // null pour articles simples (SIMPLE_ARTICLE)
    val blogTitle: String?, // null pour articles simples

    // Contenu
    val title: String,
    val excerpt: String, // Extrait de l'article (premiers caractères du contenu)
    val coverImageUrl: String?, // URL de l'image de couverture

    // Dates
    val createdAt: LocalDateTime, // Date de création

    // Navigation
    val url: String, // URL relative pour la navigation
    // Format:
    // - Pour BLOG_POST: "/blog/{blogShareId}/post/{postShareId}"
    // - Pour SIMPLE_ARTICLE: "/article/share/{shareId}"

    // Auteur
    val authorName: String, // Nom d'affichage de l'auteur
    val authorAvatar: String?, // URL de l'avatar de l'auteur
    val authorId: String, // ObjectId de l'auteur converti en String

    // Métadonnées
    val category: String?, // Catégorie de l'article
    val tags: List<String>, // Tags de l'article

    // Statistiques
    val commentCount: Long = 0, // Nombre de commentaires
    val readTime: Int, // Temps de lecture en minutes (calculé)
    val likeCount: Long = 0, // Nombre de likes
    val viewCount: Long = 0, // Nombre de vues
    val shareCount: Long = 0, // Nombre de partages

    // État utilisateur (si authentifié)
    val isLiked: Boolean = false, // L'utilisateur connecté a-t-il liké cet article ?
    val isFavorited: Boolean = false, // L'utilisateur connecté a-t-il favorisé cet article ?
    val isFollowingAuthor: Boolean = false, // L'utilisateur connecté suit-il l'auteur ?

    // Type et statut
    val type: ArticleType, // SIMPLE_ARTICLE ou BLOG_POST
    val isPublished: Boolean, // Article publié ?
    val isPrivate: Boolean // Article privé ?
)



