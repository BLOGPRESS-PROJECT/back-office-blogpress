package com.kobe.blogpress_api.services.fileStorage

import com.kobe.blogpress_api.configuration.fileStorage.FileStorageProperties
import com.kobe.blogpress_api.exception.FileStorageException
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

@Service
class FileStorageService(
    private val fileStorageProperties: FileStorageProperties
) {

    fun storeProfilePicture(file: FilePart, userId: String): Mono<String> {
        return Mono.fromCallable {
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

            // Sauvegarder le fichier
            Files.copy(file.content().toIterable().iterator().asSequence().first().asInputStream(),
                destinationPath,
                StandardCopyOption.REPLACE_EXISTING)

            // Retourner l'URL relative
            "/uploads/profile-pictures/$newFileName"
        }
    }

    fun deleteProfilePicture(fileName: String): Mono<Boolean> {
        return Mono.fromCallable {
            if (fileName.isBlank()) return@fromCallable false

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