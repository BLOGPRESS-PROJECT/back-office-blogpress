package com.kobe.blogpress_api.services.blog

import com.kobe.blogpress_api.repository.blog.BlogRepository
import kotlinx.coroutines.reactor.awaitSingle
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.text.Normalizer

@Service
class BlogSlugService(
    private val blogRepository: BlogRepository
) {

    suspend fun generateUniqueSlug(title: String, blogId: ObjectId? = null): String {
        val baseSlug = generateSlug(title)
        return ensureUniqueSlug(baseSlug, blogId)
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

    private suspend fun ensureUniqueSlug(slug: String, blogId: ObjectId? = null): String {
        var finalSlug = slug
        var counter = 1

        while (isSlugTaken(finalSlug, blogId)) {
            finalSlug = "$slug-$counter"
            counter++
        }

        return finalSlug
    }

    private suspend fun isSlugTaken(slug: String, blogId: ObjectId?): Boolean {
        return if (blogId != null) {
            blogRepository.existsBySlugAndIdNot(slug, blogId).awaitSingle()
        } else {
            blogRepository.existsBySlug(slug).awaitSingle()
        }
    }
}