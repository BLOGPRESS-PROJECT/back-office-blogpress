package com.kobe.blogpress_api.configuration

import com.kobe.blogpress_api.configuration.security.jwt.OptionalAuthenticationPrincipalResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer

@Configuration
class WebFluxConfig : WebFluxConfigurer {

    override fun configureArgumentResolvers(configurer: ArgumentResolverConfigurer) {
        configurer.addCustomResolver(
            OptionalAuthenticationPrincipalResolver()
        )
    }
}

