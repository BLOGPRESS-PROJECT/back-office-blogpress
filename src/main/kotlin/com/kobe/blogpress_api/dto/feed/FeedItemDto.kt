package com.kobe.blogpress_api.dto.feed

import com.kobe.blogpress_api.domain.model.article.ArticleType
import java.time.LocalDateTime

/**
 * DTO pour un article dans le feed principal.
 * Contient toutes les informations nécessaires pour l'affichage dans FeedItemCard.
 */
data class FeedItemDto(
    // Identifiants
    val id: String,              // ObjectId converti en String (articleId)
    val blogId: String?,         // null pour articles simples (SIMPLE_ARTICLE)
    val blogTitle: String?,      // null pour articles simples

    // 🔥 Navigation publique
    val shareId: String,         // UUID de l'article, identique à ArticleResponse.shareId
    // Optionnel : URL publique exacte (même logique que ArticleResponse.publicUrl)
    val publicUrl: String?,      // ex: "/article/{shareId}" ou "/blog/{blogShareId}/post/{postShareId}"

    // Contenu
    val title: String,
    val excerpt: String,         // Extrait de l'article (premiers caractères du contenu)
    val coverImageUrl: String?,  // URL de l'image de couverture (même convention que ArticleResponse)

    // Dates
    val createdAt: LocalDateTime, // Date de création
    val publishAt: LocalDateTime?, // ⭐ Date de publication (nullable)

    // Navigation interne (optionnelle pour le frontend)
    val url: String?,             // URL relative "SEO" optionnelle
    // Format recommandé si utilisé :
    // - BLOG_POST: "/blog/{blogShareId}/post/{postShareId}" ou "/blog/{blogSlug}/post/{postSlug}"
    // - SIMPLE_ARTICLE: "/article/{slug}" ou "/article/share/{shareId}"

    // Auteur
    val authorName: String,      // Nom d'affichage de l'auteur
    val authorAvatar: String?,   // URL de l'avatar de l'auteur
    val authorId: String,        // ObjectId de l'auteur converti en String

    // Métadonnées
    val category: String,        // Catégorie de l'article (valeur par défaut côté backend si null)
    val tags: List<String>,      // Tags de l'article

    // Statistiques (synchronisées avec ArticleResponse / interactions)
    val commentCount: Long = 0,  // Nombre de commentaires
    val readTime: Int,           // Temps de lecture en minutes (calculé côté backend)
    val likeCount: Long = 0,     // Nombre de likes
    val viewCount: Long = 0,     // Nombre de vues
    val shareCount: Long = 0,    // Nombre de partages

    // État utilisateur (si authentifié)
    val isLiked: Boolean = false,           // L'utilisateur connecté a-t-il liké cet article ?
    val isFavorited: Boolean = false,       // L'utilisateur connecté a-t-il favorisé cet article ?
    val isFollowingAuthor: Boolean = false, // L'utilisateur connecté suit-il l'auteur ?

    // Type et statut (même logique que ArticleResponse)
    val type: ArticleType,       // SIMPLE_ARTICLE ou BLOG_POST
    val isPublished: Boolean,    // Article publié ?
    val isPrivate: Boolean       // Article privé ?
)
