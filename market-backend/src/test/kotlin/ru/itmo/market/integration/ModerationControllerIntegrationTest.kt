package ru.itmo.market.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.containers.PostgreSQLContainer
import ru.itmo.market.model.enums.UserRole
import ru.itmo.market.repository.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Moderation Controller Integration Tests")
class ModerationControllerIntegrationTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:15").apply {
            withDatabaseName("itmo_market")
            withUsername("itmo_user")
            withPassword("itmo_password")
        }
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var shopRepository: ShopRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @BeforeEach
    fun setUp() {
        productRepository.deleteAll()
        shopRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    @DisplayName("should get pending products for moderator")
    fun testGetPendingProducts() {
        val moderator = testAuthHelper.createTestUser(username = "testmod", email = "testmod@example.com", roles = setOf(UserRole.USER, UserRole.MODERATOR))

        mockMvc.get("/api/moderation/products") {
            param("userId", moderator.id.toString())
            param("page", "1")
            param("pageSize", "20")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data") { isArray() }
            jsonPath("$.page") { value(1) }
            jsonPath("$.pageSize") { value(20) }
        }
    }

    @Test
    @DisplayName("should reject moderation endpoint for non-moderator")
    fun testGetPendingProductsWithoutModeratorRole() {
        val user = testAuthHelper.createTestUser(username = "testuser", email = "testuser@example.com")

        mockMvc.get("/api/moderation/products") {
            param("userId", user.id.toString())
            param("page", "1")
            param("pageSize", "20")
        }.andExpect {
            // Ожидаем 403 Forbidden, так как нет роли MODERATOR
            status { isForbidden() } 
        }
    }
}