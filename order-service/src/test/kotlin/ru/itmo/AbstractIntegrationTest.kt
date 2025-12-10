package ru.itmo.order.integration

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
abstract class AbstractIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:15")).apply {
            withDatabaseName("itmo_market_order")
            withUsername("test_user")
            withPassword("test_password")
            withReuse(true)
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName)
            
            // Disable Eureka and Config Server for integration tests to prevent connection errors
            registry.add("spring.cloud.config.enabled") { "false" }
            registry.add("eureka.client.enabled") { "false" }
        }
    }
}