package ru.itmo.user.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.containers.PostgreSQLContainer
import ru.itmo.user.model.dto.request.CreateShopRequest
import ru.itmo.user.model.dto.request.UpdateShopRequest
import ru.itmo.user.model.dto.response.PaginatedResponse
import ru.itmo.user.model.dto.response.ProductResponse
import ru.itmo.user.model.entity.Shop
import ru.itmo.user.model.enums.UserRole
import ru.itmo.user.repository.ShopRepository
import ru.itmo.user.repository.UserRepository
import ru.itmo.user.repository.UserRoleRepository
import ru.itmo.user.service.client.ProductServiceClient
import java.math.BigDecimal
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Shop Controller Integration Tests")
class ShopControllerIntegrationTest {

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
    private lateinit var shopRepository: ShopRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userRoleRepository: UserRoleRepository

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @MockBean
    private lateinit var productServiceClient: ProductServiceClient

    @BeforeEach
    fun setUp() {
        shopRepository.deleteAll().block()
        userRoleRepository.deleteAll().block()
        userRepository.deleteAll().block()

        // Default mock behavior for product service client
        whenever(productServiceClient.countProductsByShopId(any())).thenReturn(0L)
        whenever(productServiceClient.getProductsByShopId(any(), any(), any())).thenReturn(
            PaginatedResponse(
                data = emptyList(),
                page = 1,
                pageSize = 10,
                totalElements = 0,
                totalPages = 0
            )
        )
    }

    @Test
    @DisplayName("should get shops with pagination")
    fun testGetShopsWithPagination() {
        val seller1 = testAuthHelper.createTestUser(
            username = "seller1",
            email = "seller1@example.com",
            roles = setOf(UserRole.SELLER)
        )

        val seller2 = testAuthHelper.createTestUser(
            username = "seller2",
            email = "seller2@example.com",
            roles = setOf(UserRole.SELLER)
        )

        // Create some test shops
        shopRepository.save(
            Shop(
                name = "Shop 1",
                description = "Description 1",
                avatarUrl = null,
                sellerId = seller1.id
            )
        ).block()

        shopRepository.save(
            Shop(
                name = "Shop 2",
                description = "Description 2",
                avatarUrl = null,
                sellerId = seller2.id
            )
        ).block()

        webTestClient.get()
            .uri { uriBuilder ->
                uriBuilder.path("/api/shops")
                    .queryParam("page", "1")
                    .queryParam("pageSize", "20")
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data").isArray
            .jsonPath("$.page").isEqualTo(1)
            .jsonPath("$.pageSize").isEqualTo(20)
            .jsonPath("$.totalElements").isEqualTo(2)
    }

    @Test
    @DisplayName("should reject invalid pagination parameters")
    fun testGetShopsWithInvalidPagination() {
        webTestClient.get()
            .uri { uriBuilder ->
                uriBuilder.path("/api/shops")
                    .queryParam("page", "0")
                    .queryParam("pageSize", "100")
                    .build()
            }
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    @DisplayName("should get shop by id")
    fun testGetShopById() {
        val seller = testAuthHelper.createTestUser(
            username = "seller",
            email = "seller@example.com",
            roles = setOf(UserRole.SELLER)
        )

        val shop = shopRepository.save(
            Shop(
                name = "Test Shop",
                description = "Test Description",
                avatarUrl = "https://example.com/avatar.jpg",
                sellerId = seller.id
            )
        ).block()!!

        whenever(productServiceClient.countProductsByShopId(shop.id)).thenReturn(5L)

        webTestClient.get()
            .uri("/api/shops/${shop.id}")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(shop.id)
            .jsonPath("$.name").isEqualTo("Test Shop")
            .jsonPath("$.description").isEqualTo("Test Description")
            .jsonPath("$.sellerId").isEqualTo(seller.id)
            .jsonPath("$.sellerName").isEqualTo("Test User")
            .jsonPath("$.productsCount").isEqualTo(5)
    }

    @Test
    @DisplayName("should return 404 when shop not found")
    fun testGetShopByIdNotFound() {
        webTestClient.get()
            .uri("/api/shops/99999")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    @DisplayName("should get shop products")
    fun testGetShopProducts() {
        val seller = testAuthHelper.createTestUser(
            username = "seller",
            email = "seller@example.com",
            roles = setOf(UserRole.SELLER)
        )

        val shop = shopRepository.save(
            Shop(
                name = "Test Shop",
                description = "Test Description",
                avatarUrl = null,
                sellerId = seller.id
            )
        ).block()!!

        val productResponse = ProductResponse(
            id = 1L,
            name = "Test Product",
            description = "Product Description",
            price = BigDecimal("99.99"),
            imageUrl = "https://example.com/product.jpg",
            shopId = shop.id,
            sellerId = seller.id,
            status = "APPROVED",
            rejectionReason = null,
            averageRating = 4.5,
            commentsCount = 10,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        whenever(productServiceClient.getProductsByShopId(shop.id, 1, 10)).thenReturn(
            PaginatedResponse(
                data = listOf(productResponse),
                page = 1,
                pageSize = 10,
                totalElements = 1,
                totalPages = 1
            )
        )

        webTestClient.get()
            .uri { uriBuilder ->
                uriBuilder.path("/api/shops/${shop.id}/products")
                    .queryParam("page", "1")
                    .queryParam("pageSize", "10")
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data").isArray
            .jsonPath("$.data[0].id").isEqualTo(1)
            .jsonPath("$.data[0].name").isEqualTo("Test Product")
            .jsonPath("$.totalElements").isEqualTo(1)
    }

    @Test
    @DisplayName("should return 404 when getting products for non-existent shop")
    fun testGetShopProductsNotFound() {
        webTestClient.get()
            .uri { uriBuilder ->
                uriBuilder.path("/api/shops/99999/products")
                    .queryParam("page", "1")
                    .queryParam("pageSize", "10")
                    .build()
            }
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    @DisplayName("should create shop as seller")
    fun testCreateShop() {
        val seller = testAuthHelper.createTestUser(
            username = "seller",
            email = "seller@example.com",
            roles = setOf(UserRole.SELLER)
        )

        val createRequest = CreateShopRequest(
            name = "New Shop",
            description = "New Description",
            avatarUrl = "https://example.com/avatar.jpg"
        )

        webTestClient.post()
            .uri { uriBuilder ->
                uriBuilder.path("/api/shops")
                    .queryParam("userId", seller.id)
                    .build()
            }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.name").isEqualTo("New Shop")
            .jsonPath("$.description").isEqualTo("New Description")
            .jsonPath("$.sellerId").isEqualTo(seller.id)
            .jsonPath("$.productsCount").isEqualTo(0)
    }

    @Test
    @DisplayName("should return 409 when seller already has a shop")
    fun testCreateShopConflict() {
        val seller = testAuthHelper.createTestUser(
            username = "seller",
            email = "seller@example.com",
            roles = setOf(UserRole.SELLER)
        )

        // Create first shop
        shopRepository.save(
            Shop(
                name = "Existing Shop",
                description = "Description",
                avatarUrl = null,
                sellerId = seller.id
            )
        ).block()

        val createRequest = CreateShopRequest(
            name = "Second Shop",
            description = "Another Description",
            avatarUrl = null
        )

        webTestClient.post()
            .uri { uriBuilder ->
                uriBuilder.path("/api/shops")
                    .queryParam("userId", seller.id)
                    .build()
            }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus().isEqualTo(409)
    }

    @Test
    @DisplayName("should update shop as owner")
    fun testUpdateShop() {
        val seller = testAuthHelper.createTestUser(
            username = "seller",
            email = "seller@example.com",
            roles = setOf(UserRole.SELLER)
        )

        val shop = shopRepository.save(
            Shop(
                name = "Old Shop",
                description = "Old Description",
                avatarUrl = null,
                sellerId = seller.id
            )
        ).block()!!

        val updateRequest = UpdateShopRequest(
            name = "Updated Shop",
            description = "Updated Description",
            avatarUrl = "https://example.com/new-avatar.jpg"
        )

        webTestClient.put()
            .uri { uriBuilder ->
                uriBuilder.path("/api/shops/${shop.id}")
                    .queryParam("userId", seller.id)
                    .build()
            }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.name").isEqualTo("Updated Shop")
            .jsonPath("$.description").isEqualTo("Updated Description")
            .jsonPath("$.avatarUrl").isEqualTo("https://example.com/new-avatar.jpg")
    }

    @Test
    @DisplayName("should return 403 when updating shop as non-owner")
    fun testUpdateShopForbidden() {
        val seller = testAuthHelper.createTestUser(
            username = "seller",
            email = "seller@example.com",
            roles = setOf(UserRole.SELLER)
        )

        val otherUser = testAuthHelper.createTestUser(
            username = "other",
            email = "other@example.com",
            roles = setOf(UserRole.USER)
        )

        val shop = shopRepository.save(
            Shop(
                name = "Shop",
                description = "Description",
                avatarUrl = null,
                sellerId = seller.id
            )
        ).block()!!

        val updateRequest = UpdateShopRequest(
            name = "Hacked Shop",
            description = null,
            avatarUrl = null
        )

        webTestClient.put()
            .uri { uriBuilder ->
                uriBuilder.path("/api/shops/${shop.id}")
                    .queryParam("userId", otherUser.id)
                    .build()
            }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updateRequest)
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    @DisplayName("should delete shop as owner")
    fun testDeleteShopAsOwner() {
        val seller = testAuthHelper.createTestUser(
            username = "seller",
            email = "seller@example.com",
            roles = setOf(UserRole.SELLER)
        )

        val shop = shopRepository.save(
            Shop(
                name = "Shop",
                description = "Description",
                avatarUrl = null,
                sellerId = seller.id
            )
        ).block()!!

        webTestClient.delete()
            .uri { uriBuilder ->
                uriBuilder.path("/api/shops/${shop.id}")
                    .queryParam("userId", seller.id)
                    .build()
            }
            .exchange()
            .expectStatus().isNoContent

        // Verify shop is deleted
        webTestClient.get()
            .uri("/api/shops/${shop.id}")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    @DisplayName("should delete shop as admin")
    fun testDeleteShopAsAdmin() {
        val seller = testAuthHelper.createTestUser(
            username = "seller",
            email = "seller@example.com",
            roles = setOf(UserRole.SELLER)
        )

        val admin = testAuthHelper.createTestUser(
            username = "admin",
            email = "admin@example.com",
            roles = setOf(UserRole.ADMIN)
        )

        val shop = shopRepository.save(
            Shop(
                name = "Shop",
                description = "Description",
                avatarUrl = null,
                sellerId = seller.id
            )
        ).block()!!

        webTestClient.delete()
            .uri { uriBuilder ->
                uriBuilder.path("/api/shops/${shop.id}")
                    .queryParam("userId", admin.id)
                    .build()
            }
            .exchange()
            .expectStatus().isNoContent
    }

    @Test
    @DisplayName("should return 403 when deleting shop as non-owner non-admin")
    fun testDeleteShopForbidden() {
        val seller = testAuthHelper.createTestUser(
            username = "seller",
            email = "seller@example.com",
            roles = setOf(UserRole.SELLER)
        )

        val otherUser = testAuthHelper.createTestUser(
            username = "other",
            email = "other@example.com",
            roles = setOf(UserRole.USER)
        )

        val shop = shopRepository.save(
            Shop(
                name = "Shop",
                description = "Description",
                avatarUrl = null,
                sellerId = seller.id
            )
        ).block()!!

        webTestClient.delete()
            .uri { uriBuilder ->
                uriBuilder.path("/api/shops/${shop.id}")
                    .queryParam("userId", otherUser.id)
                    .build()
            }
            .exchange()
            .expectStatus().isForbidden
    }
}
