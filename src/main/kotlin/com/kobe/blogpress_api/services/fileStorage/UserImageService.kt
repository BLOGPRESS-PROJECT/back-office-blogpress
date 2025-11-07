package com.kobe.blogpress_api.services.fileStorage

import com.kobe.blogpress_api.configuration.fileStorage.FileStorageProperties
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path

/**
 * Service pour gérer les ressources d'images de profil
 *
 * NOTE: Les fichiers sont déjà servis statiquement via /uploads/ grâce à FileStorageConfig.
 * Ce service est optionnel et peut être utilisé si vous voulez un endpoint API dédié
 * avec des headers de cache personnalisés.
 */
@Service
class UserImageService(
    private val fileStorageProperties: FileStorageProperties
) {

    private val profilePicturesPath: Path = fileStorageProperties.getProfilePicturesPath()

    init {
        // Créer le répertoire s'il n'existe pas
        if (!Files.exists(profilePicturesPath)) {
            Files.createDirectories(profilePicturesPath)
        }
    }

    /**
     * Récupérer la ressource image de profil par userId
     * Cherche un fichier qui commence par "{userId}_"
     */
    suspend fun getProfilePictureResource(userId: String): Resource? {
        return try {
            val file = findProfilePictureFile(userId) ?: return null

            if (!Files.exists(file)) return null

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

    /**
     * Récupérer la ressource par nom de fichier
     *
     * @param filename Le nom du fichier (ex: "user123_abc123.jpg")
     * @return La ressource si trouvée, null sinon
     */
    suspend fun getProfilePictureResourceByFilename(filename: String): Resource? {
        return try {
            val file = profilePicturesPath.resolve(filename).normalize()

            // Sécurité : vérifier que le fichier est dans le bon répertoire
            // Évite les attaques de path traversal (../../../etc/passwd)
            if (!file.startsWith(profilePicturesPath.normalize())) {
                return null
            }

            if (!Files.exists(file)) return null

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

    /**
     * Chercher le fichier de photo de profil pour un utilisateur
     * Les fichiers sont nommés comme "{userId}_{uuid}.{ext}"
     *
     * @param userId L'ID de l'utilisateur
     * @return Le Path du fichier s'il existe, null sinon
     */
    private fun findProfilePictureFile(userId: String): Path? {
        return try {
            if (!Files.exists(profilePicturesPath)) {
                return null
            }

            // Chercher un fichier qui commence par "{userId}_"
            Files.list(profilePicturesPath)
                .filter { it.fileName.toString().startsWith("${userId}_") }
                .findFirst()
                .orElse(null)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Vérifier si une photo de profil existe pour un utilisateur
     */
    suspend fun profilePictureExists(userId: String): Boolean {
        return getProfilePictureResource(userId) != null
    }
}