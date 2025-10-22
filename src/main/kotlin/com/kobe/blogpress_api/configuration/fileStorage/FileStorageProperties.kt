package com.kobe.blogpress_api.configuration.fileStorage

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader
import org.springframework.http.codec.multipart.MultipartHttpMessageReader
import org.springframework.stereotype.Component
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.ResourceHandlerRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import jakarta.annotation.PostConstruct

@Component
@ConfigurationProperties(prefix = "file")
data class FileStorageProperties(
    var uploadDir: String = "uploads",
    var maxSize: String = "5MB",
    var allowedTypes: List<String> = listOf("image/jpeg", "image/png", "image/gif", "image/webp")
) {
    fun getUploadPath(): Path = Paths.get(uploadDir).toAbsolutePath().normalize()

    fun getProfilePicturesPath(): Path = getUploadPath().resolve("profile-pictures")

    @PostConstruct
    fun init() {
        Files.createDirectories(getUploadPath())
        Files.createDirectories(getProfilePicturesPath())
    }
}

@Configuration
@EnableWebFlux
class FileStorageConfig(
    private val fileStorageProperties: FileStorageProperties
) : WebFluxConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:${fileStorageProperties.uploadDir}/")
    }
}