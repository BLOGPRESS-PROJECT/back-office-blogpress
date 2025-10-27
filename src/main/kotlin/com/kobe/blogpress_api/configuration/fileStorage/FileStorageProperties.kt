package com.kobe.blogpress_api.configuration.fileStorage

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.ResourceHandlerRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import jakarta.annotation.PostConstruct

@Component
@ConfigurationProperties(prefix = "file.storage")
data class FileStorageProperties(
    var basePath: String = "./uploads",
    var maxFileSize: Long = 5242880, // 5MB en bytes
    var allowedTypes: List<String> = listOf("image/jpeg", "image/png", "image/gif", "image/webp"),
    var profilePicturesPath: String = "profile-pictures",
    var blogCoversPath: String = "blog-covers",
    var blogLogosPath: String = "blog-logos",
    var articleCoversPath: String = "article-covers"
) {

    // Chemins absolus
    fun getBasePath(): Path = Paths.get(basePath).toAbsolutePath().normalize()

    fun getProfilePicturesPath(): Path = getBasePath().resolve(profilePicturesPath)

    fun getBlogCoversPath(): Path = getBasePath().resolve(blogCoversPath)

    fun getBlogLogosPath(): Path = getBasePath().resolve(blogLogosPath)

    fun getArticleCoversPath(): Path = getBasePath().resolve(articleCoversPath)

    @PostConstruct
    fun init() {
        // Créer tous les répertoires au démarrage
        val directories = listOf(
            getBasePath(),
            getProfilePicturesPath(),
            getBlogCoversPath(),
            getBlogLogosPath(),
            getArticleCoversPath()
        )

        directories.forEach { dir ->
            if (!Files.exists(dir)) {
                Files.createDirectories(dir)
                println("✅ Created directory: $dir")
            } else {
                println("✓ Directory exists: $dir")
            }
        }

        println("📁 File storage initialized at: ${getBasePath()}")
    }

    // Méthodes utilitaires
    fun isValidFileType(contentType: String): Boolean {
        return allowedTypes.contains(contentType)
    }

    fun getMaxFileSizeMB(): Double {
        return maxFileSize / (1024.0 * 1024.0)
    }
}