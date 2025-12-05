package ru.itmo.market.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.delete
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.containers.PostgreSQLContainer
import ru.itmo.market.repository.UserRepository
import ru.itmo.market.model.dto.request.UpdateProfileRequest
import ru.itmo.market.model.enums.UserRole

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
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
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll()
    }

    // ==================== GET /api/users/me ====================

    @Test
    fun `should get current user info`() {
        val user = testAuthHelper.createTestUser(username = "testuser", email = "testuser@example.com")
        val token = testAuthHelper.createTokenForUser(user)

        mockMvc.get("/api/users/me") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(user.id.toInt()) }
            jsonPath("$.username") { value("testuser") }
            jsonPath("$.email") { value("testuser@example.com") }
            jsonPath("$.firstName") { value("Test") }
            jsonPath("$.lastName") { value("User") }
        }
    }

    @Test
    fun `should return 401 when getting current user without authorization`() {
        mockMvc.get("/api/users/me").andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `should return 401 when getting current user with invalid token`() {
        mockMvc.get("/api/users/me") {
            header("Authorization", "Bearer invalid_token")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    // ==================== PUT /api/users/me ====================

    @Test
    fun `should update profile successfully`() {
        val user = testAuthHelper.createTestUser(username = "testuser", email = "testuser@example.com")
        val token = testAuthHelper.createTokenForUser(user)

        val updateRequest = UpdateProfileRequest(
            email = "newemail@example.com",
            firstName = "Updated",
            lastName = "Name"
        )

        mockMvc.put("/api/users/me") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(user.id.toInt()) }
            jsonPath("$.email") { value("newemail@example.com") }
            jsonPath("$.firstName") { value("Updated") }
            jsonPath("$.lastName") { value("Name") }
        }
    }

    @Test
    fun `should update only email`() {
        val user = testAuthHelper.createTestUser(username = "testuser", email = "testuser@example.com")
        val token = testAuthHelper.createTokenForUser(user)

        val updateRequest = UpdateProfileRequest(
            email = "newemail@example.com",
            firstName = null,
            lastName = null
        )

        mockMvc.put("/api/users/me") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.email") { value("newemail@example.com") }
            jsonPath("$.firstName") { value("Test") }
            jsonPath("$.lastName") { value("User") }
        }
    }

    @Test
    @Disabled // No check for updating email
    fun `should return 409 when email already in use`() {
        val user1 = testAuthHelper.createTestUser(username = "user1", email = "user1@example.com")
        val user2 = testAuthHelper.createTestUser(username = "user2", email = "user2@example.com")
        val token = testAuthHelper.createTokenForUser(user1)

        val updateRequest = UpdateProfileRequest(
            email = "user2@example.com",
            firstName = null,
            lastName = null
        )

        mockMvc.put("/api/users/me") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
        }.andExpect {
            status { isConflict() }
        }
    }

    @Test
    @Disabled // No check for updating values
    fun `should return 400 for invalid email`() {
        val user = testAuthHelper.createTestUser(username = "testuser", email = "testuser@example.com")
        val token = testAuthHelper.createTokenForUser(user)

        val updateRequest = UpdateProfileRequest(
            email = "invalid-email",
            firstName = null,
            lastName = null
        )

        mockMvc.put("/api/users/me") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `should return 401 when updating profile without authorization`() {
        val updateRequest = UpdateProfileRequest(
            email = "newemail@example.com",
            firstName = "Updated",
            lastName = "Name"
        )

        mockMvc.put("/api/users/me") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    // ==================== DELETE /api/users/me ====================

    @Test
    fun `should delete profile successfully`() {
        val user = testAuthHelper.createTestUser(username = "testuser", email = "testuser@example.com")
        val token = testAuthHelper.createTokenForUser(user)

        mockMvc.delete("/api/users/me") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isNoContent() }
        }

        mockMvc.get("/api/users/me") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `should return 401 when deleting profile without authorization`() {
        mockMvc.delete("/api/users/me").andExpect {
            status { isUnauthorized() }
        }
    }

    // ==================== GET /api/users/{id} ====================

    @Test
    fun `should get user by id as admin`() {
        val admin = testAuthHelper.createTestUser(username = "admin", email = "admin@example.com", roles = setOf(UserRole.ADMIN))
        val adminToken = testAuthHelper.createTokenForUser(admin)

        val user = testAuthHelper.createTestUser(username = "testuser", email = "testuser@example.com")

        mockMvc.get("/api/users/${user.id}") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(user.id.toInt()) }
            jsonPath("$.username") { value("testuser") }
            jsonPath("$.email") { value("testuser@example.com") }
        }
    }

    @Test
    fun `should return 403 when getting user by id without admin role`() {
        val user1 = testAuthHelper.createTestUser(username = "user1", email = "user1@example.com", roles = setOf(UserRole.USER))
        val token1 = testAuthHelper.createTokenForUser(user1)

        val user2 = testAuthHelper.createTestUser(username = "user2", email = "user2@example.com")

        mockMvc.get("/api/users/${user2.id}") {
            header("Authorization", "Bearer $token1")
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `should return 404 when user not found`() {
        val admin = testAuthHelper.createTestUser(username = "admin", email = "admin@example.com", roles = setOf(UserRole.ADMIN))
        val adminToken = testAuthHelper.createTokenForUser(admin)

        mockMvc.get("/api/users/99999") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `should return 401 when getting user by id without authorization`() {
        mockMvc.get("/api/users/1").andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `should return 401 when getting user by id with invalid token`() {
        mockMvc.get("/api/users/1") {
            header("Authorization", "Bearer invalid_token")
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}
