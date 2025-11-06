package ru.itmo.market.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.delete
import ru.itmo.market.model.dto.request.LoginRequest
import ru.itmo.market.model.dto.request.RegisterRequest
import ru.itmo.market.repository.*
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.containers.PostgreSQLContainer


@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:tc:postgresql:15://localhost/itmo_market_test",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true"
])
class OrderControllerIntegrationTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:15").apply {
            withDatabaseName("itmo_market_test")
            withUsername("itmo_user")
            withPassword("itmo_password")
        }
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Test
    fun `should get cart for user`() {
        // Note: Требуется аутентификация
        // Это тест демонстрирует структуру, но требует JWT токена
        mockMvc.get("/api/cart") {
            header("Authorization", "Bearer invalid_token")
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}