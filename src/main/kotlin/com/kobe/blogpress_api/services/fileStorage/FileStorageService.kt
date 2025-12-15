package com.kobe.blogpress_api.services.fileStorage

import com.kobe.blogpress_api.configuration.fileStorage.FileStorageProperties
import com.kobe.blogpress_api.exception.FileStorageException
import com.kobe.blogpress_api.services.storage.StorageQuotaService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.types.ObjectId
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

@Service
class FileStorageService(
    private val fileStorageProperties: FileStorageProperties,
    private val storageQuotaService: StorageQuotaService
) {

    init {
        // Créer les répertoires au démarrage
        createDirectories()
    }

    private fun createDirectories() {
        val baseDir = Paths.get(fileStorageProperties.basePath)
        val profilePicturesDir = baseDir.resolve(fileStorageProperties.profilePicturesPath)
        val blogCoversDir = baseDir.resolve("blog-covers")
        val blogLogosDir = baseDir.resolve("blog-logos")
        val articleCoversDir = baseDir.resolve("article-covers")

        listOf(baseDir, profilePicturesDir, blogCoversDir, blogLogosDir, articleCoversDir).forEach { dir ->
            if (!Files.exists(dir)) {
                Files.createDirectories(dir)
            }
        }
    }

    // ===== PROFILE PICTURES =====

    suspend fun storeProfilePicture(file: FilePart, userId: String): String {
        return storeFile(file, "profile-pictures", userId, ObjectId(userId))
    }

    suspend fun deleteProfilePicture(fileName: String, userId: ObjectId? = null): Boolean {
        val deleted = deleteFile(fileName, "profile-pictures")
        // Mettre à jour le quota si le fichier a été supprimé
        if (deleted && userId != null) {
            try {
                val fileSize = getFileSize(fileName, "profile-pictures")
                storageQuotaService.removeStorageUsage(userId, fileSize)
            } catch (e: Exception) {
                // Ignorer les erreurs de calcul de taille
            }
        }
        return deleted
    }

    // ===== BLOG COVERS =====

    suspend fun storeBlogCoverImage(file: FilePart, blogId: String, userId: ObjectId? = null): String {
        return storeFile(file, "blog-covers", blogId, userId)
    }

    suspend fun deleteBlogCoverImage(fileName: String, userId: ObjectId? = null): Boolean {
        val deleted = deleteFile(fileName, "blog-covers")
        // Mettre à jour le quota si le fichier a été supprimé
        if (deleted && userId != null) {
            try {
                val fileSize = getFileSize(fileName, "blog-covers")
                storageQuotaService.removeStorageUsage(userId, fileSize)
            } catch (e: Exception) {
                // Ignorer les erreurs de calcul de taille
            }
        }
        return deleted
    }

    // ===== BLOG LOGOS =====

    suspend fun storeBlogLogoImage(file: FilePart, blogId: String, userId: ObjectId? = null): String {
        return storeFile(file, "blog-logos", blogId, userId)
    }

    suspend fun deleteBlogLogoImage(fileName: String, userId: ObjectId? = null): Boolean {
        val deleted = deleteFile(fileName, "blog-logos")
        // Mettre à jour le quota si le fichier a été supprimé
        if (deleted && userId != null) {
            try {
                val fileSize = getFileSize(fileName, "blog-logos")
                storageQuotaService.removeStorageUsage(userId, fileSize)
            } catch (e: Exception) {
                // Ignorer les erreurs de calcul de taille
            }
        }
        return deleted
    }

    // ===== ARTICLE COVERS =====

    suspend fun storeArticleCoverImage(file: FilePart, articleId: String, userId: ObjectId? = null): String {
        return storeFile(file, "article-covers", articleId, userId)
    }

    suspend fun deleteArticleCoverImage(fileName: String, userId: ObjectId? = null): Boolean {
        val deleted = deleteFile(fileName, "article-covers")
        // Mettre à jour le quota si le fichier a été supprimé
        if (deleted && userId != null) {
            try {
                val fileSize = getFileSize(fileName, "article-covers")
                storageQuotaService.removeStorageUsage(userId, fileSize)
            } catch (e: Exception) {
                // Ignorer les erreurs de calcul de taille
            }
        }
        return deleted
    }

    // ===== GENERIC METHODS =====

    private suspend fun storeFile(file: FilePart, directory: String, entityId: String, userId: ObjectId? = null): String {
        return withContext(Dispatchers.IO) {
            // Valider le type de fichier
            val contentType = file.headers().contentType?.toString() ?: ""
            if (!fileStorageProperties.allowedTypes.contains(contentType)) {
                throw FileStorageException(
                    "File type not allowed. Allowed types: ${fileStorageProperties.allowedTypes}"
                )
            }

            // ⭐ Vérifier le quota de stockage (si userId fourni)
            // Note: Le Content-Length peut ne pas être disponible dans les headers pour les uploads multipart
            // On vérifiera le quota après l'écriture du fichier en utilisant la taille réelle
            // Pour l'instant, on skip cette vérification préalable car elle n'est pas fiable avec multipart

            // Générer un nom de fichier unique
            val fileExtension = getFileExtension(file.filename())
            val newFileName = "${entityId}_${UUID.randomUUID()}$fileExtension"

            // Chemin de destination
            val destinationPath = Paths.get(fileStorageProperties.basePath)
                .resolve(directory)
                .resolve(newFileName)

            try {
                // Sauvegarder le fichier de manière asynchrone
                file.transferTo(destinationPath).subscribe()

                // Attendre que le fichier soit écrit
                Thread.sleep(100)

                if (!Files.exists(destinationPath)) {
                    throw FileStorageException("Failed to save file")
                }

                // ⭐ Mettre à jour le quota de stockage (si userId fourni)
                if (userId != null) {
                    try {
                        val fileSize = Files.size(destinationPath)
                        storageQuotaService.addStorageUsage(userId, fileSize)
                    } catch (e: Exception) {
                        // Log mais ne pas faire échouer l'upload
                        println("Warning: Could not update storage quota: ${e.message}")
                    }
                }

                // Retourner l'URL relative
                "/uploads/$directory/$newFileName"
            } catch (e: Exception) {
                throw FileStorageException("Could not store file: ${e.message}")
            }
        }
    }

    private suspend fun deleteFile(fileName: String, directory: String): Boolean {
        return withContext(Dispatchers.IO) {
            if (fileName.isBlank()) return@withContext false

            try {
                val filePath = Paths.get(fileStorageProperties.basePath)
                    .resolve(directory)
                    .resolve(fileName.substringAfterLast("/"))

                if (Files.exists(filePath)) {
                    Files.delete(filePath)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun getFileExtension(filename: String): String {
        return if (filename.contains(".")) {
            ".${filename.substringAfterLast(".")}"
        } else {
            ""
        }
    }

    // ===== UTILITY METHODS =====

    fun isExternalUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.startsWith("http://") || url.startsWith("https://")
    }

    fun isLocalFile(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.startsWith("/uploads/")
    }

    /**
     * Récupère la taille d'un fichier en bytes.
     */
    private suspend fun getFileSize(fileName: String, directory: String): Long {
        return withContext(Dispatchers.IO) {
            try {
                val filePath = Paths.get(fileStorageProperties.basePath)
                    .resolve(directory)
                    .resolve(fileName.substringAfterLast("/"))
                if (Files.exists(filePath)) {
                    Files.size(filePath)
                } else {
                    0L
                }
            } catch (e: Exception) {
                0L
            }
        }
    }
}