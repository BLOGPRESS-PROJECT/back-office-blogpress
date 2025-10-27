package com.kobe.blogpress_api.configuration.fileStorage

import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.ResourceHandlerRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer

@Configuration
@EnableWebFlux
class FileStorageConfig(
    private val fileStorageProperties: FileStorageProperties
) : WebFluxConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val uploadPath = fileStorageProperties.getBasePath().toString()

        // Servir tous les fichiers sous /uploads/**
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:$uploadPath/")

        println("📂 Static file serving configured:")
        println("   URL pattern: /uploads/**")
        println("   File location: $uploadPath/")
        println("   Available paths:")
        println("   - /uploads/profile-pictures/")
        println("   - /uploads/blog-covers/")
        println("   - /uploads/blog-logos/")
        println("   - /uploads/article-covers/")
    }
}