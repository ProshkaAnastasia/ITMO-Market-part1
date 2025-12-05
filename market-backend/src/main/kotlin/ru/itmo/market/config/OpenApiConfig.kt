package ru.itmo.market.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
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
            addSecurityItem(SecurityRequirement().addList("bearer-jwt"))
            components = io.swagger.v3.oas.models.Components() 
            components.addSecuritySchemes(
                "bearer-jwt",
                SecurityScheme().apply {
                    type = SecurityScheme.Type.HTTP
                    scheme = "bearer"
                    bearerFormat = "JWT"
                    description = "JWT Authentication Token"
                }
            )
        }
    }

}