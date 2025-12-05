package ru.itmo.market.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser // ✅ FIX: Необходимый импорт для мокирования пользователя
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.containers.PostgreSQLContainer
import ru.itmo.market.repository.*
// import ru.itmo.market.model.enums.UserRole // Больше не нужен
// import ru.itmo.market.security.jwt.JwtTokenProvider // ❌ УДАЛЕН

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

    // ❌ УДАЛЕНО, так как JWT удален
    // @Autowired
    // private lateinit var testAuthHelper: TestAuthHelper

    // ❌ УДАЛЕНО, так как JWT удален
    // @Autowired
    // private lateinit var jwtTokenProvider: JwtTokenProvider

    @BeforeEach
    fun setUp() {
        productRepository.deleteAll()
        shopRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    @DisplayName("should get pending products for moderator")
    @WithMockUser(username = "moderator", roles = ["MODERATOR"]) // ✅ Имитируем запрос от аутентифицированного модератора
    fun testGetPendingProducts() {
        // Убрана вся логика создания пользователя и токена

        mockMvc.get("/api/moderation/products") {
            // Убран заголовок Authorization
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
    @WithMockUser(username = "user", roles = ["USER"]) // ✅ Имитируем запрос от обычного пользователя
    fun testGetPendingProductsWithoutModeratorRole() {
        // Убрана вся логика создания пользователя и токена

        mockMvc.get("/api/moderation/products") {
            // Убран заголовок Authorization
            param("page", "1")
            param("pageSize", "20")
        }.andExpect {
            // Ожидаем 403 Forbidden, так как нет роли MODERATOR
            status { isForbidden() } 
        }
    }
}