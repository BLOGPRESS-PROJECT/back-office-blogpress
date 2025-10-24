package com.kobe.blogpress_api.services.fileStorage

import com.kobe.blogpress_api.configuration.fileStorage.FileStorageProperties
import com.kobe.blogpress_api.exception.FileStorageException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.util.*

@Service
class FileStorageService(
    private val fileStorageProperties: FileStorageProperties
) {

    suspend fun storeProfilePicture(file: FilePart, userId: String): String {
        return withContext(Dispatchers.IO) {
            // Valider le type de fichier
            val contentType = file.headers().contentType?.toString() ?: ""
            if (!fileStorageProperties.allowedTypes.contains(contentType)) {
                throw FileStorageException("File type not allowed. Allowed types: ${fileStorageProperties.allowedTypes}")
            }

            // Générer un nom de fichier unique
            val fileExtension = getFileExtension(file.filename())
            val newFileName = "${userId}_${UUID.randomUUID()}$fileExtension"

            // Chemin de destination
            val destinationPath = fileStorageProperties.getProfilePicturesPath().resolve(newFileName)

            // Sauvegarder le fichier de manière asynchrone
            file.transferTo(destinationPath).subscribe()

            // Alternative si transferTo ne marche pas bien
            // Attendre que tous les buffers soient écrits
            Thread.sleep(100) // Petit délai pour s'assurer que le fichier est écrit

            if (!Files.exists(destinationPath)) {
                throw FileStorageException("Failed to save file")
            }

            // Retourner l'URL relative
            "/uploads/profile-pictures/$newFileName"
        }
    }

    suspend fun deleteProfilePicture(fileName: String): Boolean {
        return withContext(Dispatchers.IO) {
            if (fileName.isBlank()) return@withContext false

            val filePath = fileStorageProperties.getProfilePicturesPath()
                .resolve(fileName.substringAfterLast("/"))

            if (Files.exists(filePath)) {
                Files.delete(filePath)
                true
            } else {
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
}