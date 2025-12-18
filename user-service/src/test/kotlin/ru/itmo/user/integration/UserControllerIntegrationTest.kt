package ru.itmo.user.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.containers.PostgreSQLContainer
import ru.itmo.user.repository.UserRepository
import ru.itmo.user.repository.UserRoleRepository
import ru.itmo.user.model.dto.request.UpdateProfileRequest
import ru.itmo.user.model.enums.UserRole

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Testcontainers
class UserControllerIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:15").apply {
            withDatabaseName("itmo_market")
            withUsername("itmo_user")
            withPassword("itmo_password")
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") {
                "r2dbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/${postgres.databaseName}"
            }
            registry.add("spring.r2dbc.username", postgres::getUsername)
            registry.add("spring.r2dbc.password", postgres::getPassword)
            registry.add("spring.flyway.url", postgres::getJdbcUrl)
            registry.add("spring.flyway.user", postgres::getUsername)
            registry.add("spring.flyway.password", postgres::getPassword)
        }
    }

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userRoleRepository: UserRoleRepository

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @BeforeEach
    fun setUp() {
        userRoleRepository.deleteAll().block()
        userRepository.deleteAll().block()
    }

    @Test
    fun `should get current user info`() {
        val user = testAuthHelper.createTestUser(username = "testuser", email = "testuser@example.com")

        webTestClient.get()
            .uri { uriBuilder ->
                uriBuilder.path("/api/users/me")
                    .queryParam("userId", user.id)
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(user.id)
            .jsonPath("$.username").isEqualTo("testuser")
            .jsonPath("$.email").isEqualTo("testuser@example.com")
            .jsonPath("$.firstName").isEqualTo("Test")
            .jsonPath("$.lastName").isEqualTo("User")
            .jsonPath("$.roles").isArray
    }

    @Test
    @Disabled("Authentication disabled for testing")
    fun `should return 401 when getting current user without authorization`() {
        webTestClient.get()
            .uri("/api/users/me")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `should update profile successfully`() {
        val user = testAuthHelper.createTestUser(username = "testuser", email = "testuser@example.com")

        val updateRequest = UpdateProfileRequest(
            email = "newemail@example.com",
            firstName = "Updated",
            lastName = "Name"
        )

        webTestClient.put()
            .uri { uriBuilder ->
                uriBuilder.path("/api/users/me")
                    .queryParam("userId", user.id)
                    .build()
            }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(user.id)
            .jsonPath("$.email").isEqualTo("newemail@example.com")
            .jsonPath("$.firstName").isEqualTo("Updated")
            .jsonPath("$.lastName").isEqualTo("Name")
    }

    @Test
    fun `should update only email`() {
        val user = testAuthHelper.createTestUser(username = "testuser", email = "testuser@example.com")

        val updateRequest = UpdateProfileRequest(
            email = "newemail@example.com",
            firstName = null,
            lastName = null
        )

        webTestClient.put()
            .uri { uriBuilder ->
                uriBuilder.path("/api/users/me")
                    .queryParam("userId", user.id)
                    .build()
            }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.email").isEqualTo("newemail@example.com")
            .jsonPath("$.firstName").isEqualTo("Test")
            .jsonPath("$.lastName").isEqualTo("User")
    }

    @Test
    fun `should return 409 when email already in use`() {
        val user1 = testAuthHelper.createTestUser(username = "user1", email = "user1@example.com")
        val user2 = testAuthHelper.createTestUser(username = "user2", email = "user2@example.com")

        val updateRequest = UpdateProfileRequest(
            email = "user2@example.com",
            firstName = null,
            lastName = null
        )

        webTestClient.put()
            .uri { uriBuilder ->
                uriBuilder.path("/api/users/me")
                    .queryParam("userId", user1.id)
                    .build()
            }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus().isEqualTo(409)
    }

    @Test
    fun `should return 400 for invalid email`() {
        val user = testAuthHelper.createTestUser(username = "testuser", email = "testuser@example.com")

        val updateRequest = UpdateProfileRequest(
            email = "invalid-email",
            firstName = null,
            lastName = null
        )

        webTestClient.put()
            .uri { uriBuilder ->
                uriBuilder.path("/api/users/me")
                    .queryParam("userId", user.id)
                    .build()
            }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    @Disabled("Authentication disabled for testing")
    fun `should return 401 when updating profile without authorization`() {
        val updateRequest = UpdateProfileRequest(
            email = "newemail@example.com",
            firstName = "Updated",
            lastName = "Name"
        )

        webTestClient.put()
            .uri("/api/users/me")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `should delete profile successfully`() {
        val user = testAuthHelper.createTestUser(username = "testuser", email = "testuser@example.com")

        webTestClient.delete()
            .uri { uriBuilder ->
                uriBuilder.path("/api/users/me")
                    .queryParam("userId", user.id)
                    .build()
            }
            .exchange()
            .expectStatus().isNoContent

        webTestClient.get()
            .uri { uriBuilder ->
                uriBuilder.path("/api/users/me")
                    .queryParam("userId", user.id)
                    .build()
            }
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    @Disabled("Authentication disabled for testing")
    fun `should return 401 when deleting profile without authorization`() {
        webTestClient.delete()
            .uri("/api/users/me")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `should get user by id as admin`() {
        val admin = testAuthHelper.createTestUser(
            username = "admin",
            email = "admin@example.com",
            roles = setOf(UserRole.ADMIN)
        )

        val user = testAuthHelper.createTestUser(username = "testuser", email = "testuser@example.com")

        webTestClient.get()
            .uri { uriBuilder ->
                uriBuilder.path("/api/users/${user.id}")
                    .queryParam("adminId", admin.id)
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(user.id)
            .jsonPath("$.username").isEqualTo("testuser")
            .jsonPath("$.email").isEqualTo("testuser@example.com")
    }

    @Test
    fun `should return 403 when getting user by id without admin role`() {
        val user1 = testAuthHelper.createTestUser(
            username = "user1",
            email = "user1@example.com",
            roles = setOf(UserRole.USER)
        )

        val user2 = testAuthHelper.createTestUser(username = "user2", email = "user2@example.com")

        webTestClient.get()
            .uri { uriBuilder ->
                uriBuilder.path("/api/users/${user2.id}")
                    .queryParam("adminId", user1.id)
                    .build()
            }
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `should return 404 when user not found`() {
        val admin = testAuthHelper.createTestUser(
            username = "admin",
            email = "admin@example.com",
            roles = setOf(UserRole.ADMIN)
        )

        webTestClient.get()
            .uri { uriBuilder ->
                uriBuilder.path("/api/users/99999")
                    .queryParam("adminId", admin.id)
                    .build()
            }
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    @Disabled("Authentication disabled for testing")
    fun `should return 401 when getting user by id without authorization`() {
        webTestClient.get()
            .uri("/api/users/1")
            .exchange()
            .expectStatus().isUnauthorized
    }
}
