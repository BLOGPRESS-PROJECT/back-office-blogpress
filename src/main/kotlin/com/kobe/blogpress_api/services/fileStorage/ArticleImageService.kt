package com.kobe.blogpress_api.services.fileStorage

import com.kobe.blogpress_api.configuration.fileStorage.FileStorageProperties
import com.kobe.blogpress_api.repository.article.ArticleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import org.bson.types.ObjectId
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Service
class ArticleImageService(
    private val fileStorageProperties: FileStorageProperties,
    private val articleRepository: ArticleRepository
) {

    private val articleCoversPath: Path = fileStorageProperties.getArticleCoversPath()

    init {
        // Créer le répertoire s'il n'existe pas
        if (!Files.exists(articleCoversPath)) {
            Files.createDirectories(articleCoversPath)
        }
    }

    /**
     * Récupérer la ressource de l'image de couverture d'un article par son ID
     */
    suspend fun getCoverImageResource(articleId: String): Resource? {
        return withContext(Dispatchers.IO) {
            try {
                val article = articleRepository.findById(ObjectId(articleId)).awaitSingleOrNull()
                    ?: return@withContext null
                val coverImageUrl = article.coverImageUrl ?: return@withContext null
                
                // Extraire le nom de fichier du chemin
                val filename = coverImageUrl.substringAfterLast("/")
                getCoverImageResourceByFilename(filename)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Récupérer la ressource de l'image de couverture par nom de fichier
     */
    suspend fun getCoverImageResourceByFilename(filename: String): Resource? {
        return withContext(Dispatchers.IO) {
            try {
                val file = articleCoversPath.resolve(filename).normalize()
                
                // Sécurité : vérifier que le fichier est dans le bon répertoire
                // Évite les attaques de path traversal (../../../etc/passwd)
                if (!file.startsWith(articleCoversPath.normalize())) {
                    return@withContext null
                }
                
                if (!Files.exists(file)) {
                    return@withContext null
                }
                
                val resource = UrlResource(file.toUri())
                
                if (resource.exists() && resource.isReadable) {
                    resource
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Vérifier si une image de couverture existe
     */
    suspend fun coverImageExists(articleId: String): Boolean {
        return getCoverImageResource(articleId)?.exists() ?: false
    }
}

