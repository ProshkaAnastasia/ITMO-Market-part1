package ru.itmo.user.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI().apply {
            info = Info().apply {
                title = "ITMO-Market API"
                description = "REST API для маркетплейса ITMO-Market"
                version = "1.0.0"
            }
            components = io.swagger.v3.oas.models.Components()
        }
    }

}