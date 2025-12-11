package ru.itmo.gateway.config

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
                title = "ITMO-Market API Gateway"
                description = "Объединенная API документация для ITMO-Market микросервисов"
                version = "1.0.0"
            }
        }
    }
}
