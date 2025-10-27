package com.kobe.blogpress_api.services.fileStorage

import com.kobe.blogpress_api.configuration.fileStorage.FileStorageProperties
import com.kobe.blogpress_api.exception.FileStorageException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

@Service
class FileStorageService(
    private val fileStorageProperties: FileStorageProperties
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
        return storeFile(file, "profile-pictures", userId)
    }

    suspend fun deleteProfilePicture(fileName: String): Boolean {
        return deleteFile(fileName, "profile-pictures")
    }

    // ===== BLOG COVERS =====

    suspend fun storeBlogCoverImage(file: FilePart, blogId: String): String {
        return storeFile(file, "blog-covers", blogId)
    }

    suspend fun deleteBlogCoverImage(fileName: String): Boolean {
        return deleteFile(fileName, "blog-covers")
    }

    // ===== BLOG LOGOS =====

    suspend fun storeBlogLogoImage(file: FilePart, blogId: String): String {
        return storeFile(file, "blog-logos", blogId)
    }

    suspend fun deleteBlogLogoImage(fileName: String): Boolean {
        return deleteFile(fileName, "blog-logos")
    }

    // ===== ARTICLE COVERS =====

    suspend fun storeArticleCoverImage(file: FilePart, articleId: String): String {
        return storeFile(file, "article-covers", articleId)
    }

    suspend fun deleteArticleCoverImage(fileName: String): Boolean {
        return deleteFile(fileName, "article-covers")
    }

    // ===== GENERIC METHODS =====

    private suspend fun storeFile(file: FilePart, directory: String, entityId: String): String {
        return withContext(Dispatchers.IO) {
            // Valider le type de fichier
            val contentType = file.headers().contentType?.toString() ?: ""
            if (!fileStorageProperties.allowedTypes.contains(contentType)) {
                throw FileStorageException(
                    "File type not allowed. Allowed types: ${fileStorageProperties.allowedTypes}"
                )
            }

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
}