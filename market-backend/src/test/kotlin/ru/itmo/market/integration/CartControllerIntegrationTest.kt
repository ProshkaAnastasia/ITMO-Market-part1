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
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.delete
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.containers.PostgreSQLContainer
import ru.itmo.market.repository.*
import ru.itmo.market.model.entity.Shop
import ru.itmo.market.model.entity.Product
import ru.itmo.market.model.dto.request.AddToCartRequest
import ru.itmo.market.model.dto.request.UpdateQuantityRequest
import ru.itmo.market.model.dto.response.OrderResponse
import ru.itmo.market.model.entity.Order
import ru.itmo.market.model.entity.OrderItem
import ru.itmo.market.model.enums.UserRole
import ru.itmo.market.model.enums.ProductStatus
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class CartControllerIntegrationTest {

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
    private lateinit var orderItemRepository: OrderItemRepository

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var shopRepository: ShopRepository

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @BeforeEach
    fun setUp() {
        orderItemRepository.deleteAll()
        orderRepository.deleteAll()
        productRepository.deleteAll()
        shopRepository.deleteAll()
        userRepository.deleteAll()
    }

    // ==================== GET /api/cart ====================

    @Test
    fun `should get empty cart for authorized user`() {
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
    fun `should reject cart request without authorization`() {
        mockMvc.get("/api/cart").andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `should reject cart request with invalid token`() {
        mockMvc.get("/api/cart") {
            header("Authorization", "Bearer invalid_token_12345")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    // ==================== POST /api/cart/items ====================

    @Test
    fun `should add product to cart successfully`() {
        val user = testAuthHelper.createTestUser()
        val token = testAuthHelper.createTokenForUser(user)

        val seller = testAuthHelper.createTestUser(username = "seller", email = "seller@example.com", roles = setOf(UserRole.SELLER))
        val shop = shopRepository.save(
            Shop(
                name = "Test Shop",
                sellerId = seller.id
            )
        )
        val product = productRepository.save(
            Product(
                name = "Test Product",
                price = BigDecimal("100.00"),
                shopId = shop.id,
                sellerId = seller.id,
                status = ProductStatus.APPROVED
            )
        )

        val requestBody = AddToCartRequest(productId = product.id, quantity = 2)

        mockMvc.post("/api/cart/items") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(requestBody)
        }.andExpect {
            status { isOk() }
            jsonPath("$.items") { isArray() }
            jsonPath("$.items[0].product.id") { value(product.id.toInt()) }
            jsonPath("$.items[0].quantity") { value(2) }
        }
    }

    @Test
    fun `should reject add to cart without authorization`() {
        val requestBody = AddToCartRequest(productId = 1L, quantity = 1)

        mockMvc.post("/api/cart/items") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(requestBody)
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `should return 404 when product not found`() {
        val user = testAuthHelper.createTestUser()
        val token = testAuthHelper.createTokenForUser(user)

        val requestBody = AddToCartRequest(productId = 99999L, quantity = 1)

        mockMvc.post("/api/cart/items") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(requestBody)
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `should return 400 for invalid quantity`() {
        val user = testAuthHelper.createTestUser()
        val token = testAuthHelper.createTokenForUser(user)

        val requestBody = AddToCartRequest(productId = 1L, quantity = 0)

        mockMvc.post("/api/cart/items") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(requestBody)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    // ==================== PUT /api/cart/items/{itemId} ====================

    @Test
    fun `should update cart item quantity successfully`() {
        val user = testAuthHelper.createTestUser()
        val token = testAuthHelper.createTokenForUser(user)

        val seller = testAuthHelper.createTestUser(username = "seller", email = "seller@example.com", roles = setOf(UserRole.SELLER))
        val shop = shopRepository.save(
            Shop(
                name = "Test Shop",
                description = "Shop Desc",
                avatarUrl = null,
                sellerId = seller.id
            )
        )
        val product = productRepository.save(
            Product(
                name = "Test Product",
                description = "Desc",
                price = BigDecimal("100.00"),
                imageUrl = null,
                shopId = shop.id,
                sellerId = seller.id,
                status = ProductStatus.APPROVED
            )
        )

        val addRequest = AddToCartRequest(productId = product.id, quantity = 2)
        mockMvc.post("/api/cart/items") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(addRequest)
        }

        val updateRequest = UpdateQuantityRequest(quantity = 5)
        mockMvc.put("/api/cart/items/${product.id}") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.items[0].quantity") { value(5) }
        }
    }

    @Test
    fun `should return 200 for zero quantity update`() {
        val user = testAuthHelper.createTestUser()
        val token = testAuthHelper.createTokenForUser(user)
        val seller = testAuthHelper.createTestUser(username = "seller", email = "seller@example.com", roles = setOf(UserRole.SELLER))
        val shop = shopRepository.save(
            Shop(
                name = "Test Shop",
                description = "Shop Desc",
                avatarUrl = null,
                sellerId = seller.id
            )
        )
        val product = productRepository.save(
            Product(
                name = "Test Product",
                description = "Desc",
                price = BigDecimal("100.00"),
                imageUrl = null,
                shopId = shop.id,
                sellerId = seller.id,
                status = ProductStatus.APPROVED
            )
        )

        val addRequest = AddToCartRequest(productId = product.id, quantity = 1)
        val response = mockMvc.post("/api/cart/items") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(addRequest)
        }.andExpect {
            status { isOk() }
        }.andReturn()

        val responseBody = response.response.contentAsString
        val orderResponse = objectMapper.readValue(responseBody, OrderResponse::class.java)
        val orderItems = orderResponse.items
        assert(orderItems.isNotEmpty())

        val updateRequest = UpdateQuantityRequest(quantity = 0)

        mockMvc.put("/api/cart/items/${orderItems[0].id}") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(0) }
        }
    }

    @Test
    fun `should return 404 when updating non-existent cart item`() {
        val user = testAuthHelper.createTestUser()
        val token = testAuthHelper.createTokenForUser(user)

        val updateRequest = UpdateQuantityRequest(quantity = 5)

        mockMvc.put("/api/cart/items/99999") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
        }.andExpect {
            status { isNotFound() }
        }
    }

    // ==================== DELETE /api/cart/items/{itemId} ====================

    @Test
    fun `should remove item from cart successfully`() {
        val user = testAuthHelper.createTestUser()
        val token = testAuthHelper.createTokenForUser(user)

        val seller = testAuthHelper.createTestUser(username = "seller", email = "seller@example.com", roles = setOf(UserRole.SELLER))
        val shop = shopRepository.save(
            Shop(
                name = "Test Shop",
                description = "Shop Desc",
                avatarUrl = null,
                sellerId = seller.id
            )
        )
        val product = productRepository.save(
            Product(
                name = "Test Product",
                description = "Desc",
                price = BigDecimal("100.00"),
                imageUrl = null,
                shopId = shop.id,
                sellerId = seller.id,
                status = ProductStatus.APPROVED
            )
        )

        val addRequest = AddToCartRequest(productId = product.id, quantity = 2)
        mockMvc.post("/api/cart/items") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(addRequest)
        }

        mockMvc.delete("/api/cart/items/${product.id}") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.items") { isArray() }
            jsonPath("$.items.length()") { value(0) }
        }
    }

    @Test
    fun `should return 404 when removing non-existent cart item`() {
        val user = testAuthHelper.createTestUser()
        val token = testAuthHelper.createTokenForUser(user)

        mockMvc.delete("/api/cart/items/99999") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `should return 401 when removing without authorization`() {
        mockMvc.delete("/api/cart/items/1").andExpect {
            status { isUnauthorized() }
        }
    }

    // ==================== DELETE /api/cart ====================

    @Test
    fun `should clear cart successfully`() {
        val user = testAuthHelper.createTestUser()
        val token = testAuthHelper.createTokenForUser(user)

        val seller = testAuthHelper.createTestUser(username = "seller", email = "seller@example.com", roles = setOf(UserRole.SELLER))
        val shop = shopRepository.save(
            Shop(
                name = "Test Shop",
                description = "Shop Desc",
                avatarUrl = null,
                sellerId = seller.id
            )
        )
        repeat(3) { index ->
            val product = productRepository.save(
                Product(
                    name = "Product $index",
                    description = "Desc",
                    price = BigDecimal("100.00"),
                    imageUrl = null,
                    shopId = shop.id,
                    sellerId = seller.id,
                    status = ProductStatus.APPROVED
                )
            )
            val addRequest = AddToCartRequest(productId = product.id, quantity = 1)
            mockMvc.post("/api/cart/items") {
                header("Authorization", "Bearer $token")
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(addRequest)
            }
        }

        mockMvc.delete("/api/cart") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isNoContent() }
        }

        mockMvc.get("/api/cart") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(0) }
        }
    }

    @Test
    fun `should return 401 when clearing cart without authorization`() {
        mockMvc.delete("/api/cart").andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `should return 204 when clearing empty cart`() {
        val user = testAuthHelper.createTestUser()
        val token = testAuthHelper.createTokenForUser(user)
        val cart = orderRepository.save(Order(
            userId = user.id
        ))

        mockMvc.delete("/api/cart") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isNoContent() }
        }
    }
}
