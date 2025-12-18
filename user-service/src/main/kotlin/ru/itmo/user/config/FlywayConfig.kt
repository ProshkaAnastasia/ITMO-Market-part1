package ru.itmo.user.config

import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FlywayConfig {

    @Bean
    @ConditionalOnProperty(name = ["spring.flyway.enabled"], havingValue = "true")
    fun flyway(
        @Value("\${spring.r2dbc.url}") r2dbcUrl: String,
        @Value("\${spring.r2dbc.username}") username: String,
        @Value("\${spring.r2dbc.password}") password: String,
        @Value("\${spring.flyway.locations}") locations: String,
        @Value("\${spring.flyway.schemas}") schemas: String,
        @Value("\${spring.flyway.baseline-on-migrate:false}") baselineOnMigrate: Boolean,
        @Value("\${spring.flyway.baseline-version:1}") baselineVersion: String
    ): Flyway {
        // Преобразуем r2dbc URL в JDBC URL
        // r2dbc:postgresql://host:port/db → jdbc:postgresql://host:port/db
        val jdbcUrl = r2dbcUrl.replace("r2dbc:", "jdbc:")

        val flyway = Flyway.configure()
            .dataSource(jdbcUrl, username, password)
            .locations(locations)
            .schemas(schemas)
            .baselineOnMigrate(baselineOnMigrate)
            .baselineVersion(baselineVersion)
            .load()

        flyway.migrate()

        return flyway
    }
}