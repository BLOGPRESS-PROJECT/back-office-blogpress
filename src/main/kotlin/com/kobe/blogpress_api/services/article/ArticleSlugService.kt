package com.kobe.blogpress_api.services.article

import com.kobe.blogpress_api.repository.article.ArticleRepository
import kotlinx.coroutines.reactor.awaitSingle
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.text.Normalizer

@Service
class ArticleSlugService(
    private val articleRepository: ArticleRepository
) {

    suspend fun generateUniqueSlug(
        title: String,
        articleId: ObjectId? = null,
        blogId: ObjectId? = null
    ): String {
        val baseSlug = generateSlug(title)
        return ensureUniqueSlug(baseSlug, articleId, blogId)
    }

    private fun generateSlug(title: String): String {
        return Normalizer.normalize(title, Normalizer.Form.NFD)
            .replace("\\p{M}".toRegex(), "") // Supprime les accents
            .lowercase()
            .replace("[^a-z0-9\\s-]".toRegex(), "") // Supprime caractères spéciaux
            .replace("\\s+".toRegex(), "-") // Remplace espaces par tirets
            .replace("-+".toRegex(), "-") // Supprime tirets multiples
            .trim('-') // Supprime tirets en début/fin
    }

    private suspend fun ensureUniqueSlug(slug: String, articleId: ObjectId?, blogId: ObjectId?): String {
        var finalSlug = slug
        var counter = 1

        while (isSlugTaken(finalSlug, articleId, blogId)) {
            finalSlug = "$slug-$counter"
            counter++
        }

        return finalSlug
    }

    private suspend fun isSlugTaken(slug: String, articleId: ObjectId?, blogId: ObjectId?): Boolean {
        return if (blogId != null) {
            // Pour les BLOG_POST, vérifier l'unicité dans le contexte du blog
            if (articleId != null) {
                articleRepository.existsByBlogIdAndSlugAndIdNot(blogId, slug, articleId).awaitSingle()
            } else {
                articleRepository.existsByBlogIdAndSlug(blogId, slug).awaitSingle()
            }
        } else {
            // Pour les SIMPLE_ARTICLE, vérifier l'unicité globale
            if (articleId != null) {
                articleRepository.existsBySlugAndIdNot(slug, articleId).awaitSingle()
            } else {
                articleRepository.existsBySlug(slug).awaitSingle()
            }
        }
    }
}