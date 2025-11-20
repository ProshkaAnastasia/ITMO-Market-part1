package ru.itmo.market.integration

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
import ru.itmo.market.repository.UserRepository

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("User Controller Integration Tests")
class UserControllerIntegrationTest {

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
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll()
    }

    @Test
    @DisplayName("should get current user profile when authorized")
    fun testGetCurrentUserProfile() {
        val user = testAuthHelper.createTestUser()
        val token = testAuthHelper.createTokenForUser(user)

        mockMvc.get("/api/users/me") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(user.id.toInt()) }
            jsonPath("$.username") { value(user.username) }
            jsonPath("$.email") { value(user.email) }
        }
    }

    @Test
    @DisplayName("should reject profile request without authorization")
    fun testGetProfileWithoutAuthorization() {
        mockMvc.get("/api/users/me").andExpect {
            status { isUnauthorized() }
        }
    }
}
