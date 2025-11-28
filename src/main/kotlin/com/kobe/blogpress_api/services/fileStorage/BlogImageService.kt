package com.kobe.blogpress_api.services.fileStorage

import com.kobe.blogpress_api.configuration.fileStorage.FileStorageProperties
import com.kobe.blogpress_api.services.blog.BlogService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.types.ObjectId
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Service
class BlogImageService(
    private val fileStorageProperties: FileStorageProperties,
    private val blogService: BlogService
) {

    private val blogCoversPath: Path = fileStorageProperties.getBlogCoversPath()
    private val blogLogosPath: Path = fileStorageProperties.getBlogLogosPath()

    init {
        // Créer les répertoires s'ils n'existent pas
        if (!Files.exists(blogCoversPath)) {
            Files.createDirectories(blogCoversPath)
        }
        if (!Files.exists(blogLogosPath)) {
            Files.createDirectories(blogLogosPath)
        }
    }

    /**
     * Récupérer la ressource de l'image de couverture d'un blog par son ID
     */
    suspend fun getCoverImageResource(blogId: String): Resource? {
        return withContext(Dispatchers.IO) {
            try {
                val blog = blogService.getBlogById(ObjectId(blogId))
                val coverImageUrl = blog.coverImageUrl ?: return@withContext null
                
                // Extraire le nom de fichier du chemin
                val filename = coverImageUrl.substringAfterLast("/")
                getCoverImageResourceByFilename(filename)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Récupérer la ressource de l'image logo d'un blog par son ID
     */
    suspend fun getLogoImageResource(blogId: String): Resource? {
        return withContext(Dispatchers.IO) {
            try {
                val blog = blogService.getBlogById(ObjectId(blogId))
                val logoImageUrl = blog.logoImageUrl ?: return@withContext null
                
                // Extraire le nom de fichier du chemin
                val filename = logoImageUrl.substringAfterLast("/")
                getLogoImageResourceByFilename(filename)
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
                val file = blogCoversPath.resolve(filename).normalize()
                
                // Sécurité : vérifier que le fichier est dans le bon répertoire
                // Évite les attaques de path traversal (../../../etc/passwd)
                if (!file.startsWith(blogCoversPath.normalize())) {
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
     * Récupérer la ressource de l'image logo par nom de fichier
     */
    suspend fun getLogoImageResourceByFilename(filename: String): Resource? {
        return withContext(Dispatchers.IO) {
            try {
                val file = blogLogosPath.resolve(filename).normalize()
                
                // Sécurité : vérifier que le fichier est dans le bon répertoire
                if (!file.startsWith(blogLogosPath.normalize())) {
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
    suspend fun coverImageExists(blogId: String): Boolean {
        return getCoverImageResource(blogId)?.exists() ?: false
    }

    /**
     * Vérifier si une image logo existe
     */
    suspend fun logoImageExists(blogId: String): Boolean {
        return getLogoImageResource(blogId)?.exists() ?: false
    }
}

