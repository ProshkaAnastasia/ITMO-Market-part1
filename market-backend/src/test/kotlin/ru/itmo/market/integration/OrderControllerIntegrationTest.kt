package ru.itmo.market.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.containers.PostgreSQLContainer
import ru.itmo.market.repository.UserRepository
import ru.itmo.market.repository.OrderRepository
import ru.itmo.market.model.dto.request.UpdateQuantityRequest

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Order Controller Integration Tests")
class OrderControllerIntegrationTest {

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
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @BeforeEach
    fun setUp() {
        orderRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    @DisplayName("should get cart for authorized user")
    fun testGetCartForAuthorizedUser() {
        val user = testAuthHelper.createTestUser()
        val token = testAuthHelper.createTokenForUser(user)

        mockMvc.get("/api/cart") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { isNumber() }
            jsonPath("$.userId") { value(user.id.toInt()) }
            jsonPath("$.items") { isArray() }
            jsonPath("$.status") { value("CART") }
        }
    }

    @Test
    @DisplayName("should reject cart request without authorization")
    fun testGetCartWithoutAuthorization() {
        mockMvc.get("/api/cart") {
            // No Authorization
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    @DisplayName("should reject cart request with invalid token")
    fun testGetCartWithInvalidToken() {
        mockMvc.get("/api/cart") {
            header("Authorization", "Bearer invalid_token_12345")
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}
