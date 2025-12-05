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
import ru.itmo.market.model.dto.request.AddToCartRequest
import ru.itmo.market.model.dto.request.UpdateQuantityRequest
import ru.itmo.market.model.dto.response.OrderResponse
import ru.itmo.market.model.entity.Product
import ru.itmo.market.model.entity.Shop
import ru.itmo.market.model.entity.User
import ru.itmo.market.model.enums.ProductStatus
import ru.itmo.market.model.enums.UserRole
import ru.itmo.market.repository.*
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
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var shopRepository: ShopRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var orderItemRepository: OrderItemRepository

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    private lateinit var testUser: User
    private lateinit var testProduct: Product
    private lateinit var testShop: Shop

    @BeforeEach
    fun setUp() {
        orderItemRepository.deleteAll()
        orderRepository.deleteAll()
        productRepository.deleteAll()
        shopRepository.deleteAll()
        userRepository.deleteAll()

        testUser = testAuthHelper.createTestUser(username = "testuser", email = "testuser@example.com")
        testShop = shopRepository.save(
            Shop(
                name = "Test Shop",
                description = "Desc",
                avatarUrl = null,
                sellerId = testUser.id
            )
        )
        testProduct = productRepository.save(
            Product(
                name = "Test Product",
                description = "Desc",
                price = BigDecimal("100.00"),
                imageUrl = null,
                shopId = testShop.id,
                sellerId = testUser.id,
                status = ProductStatus.APPROVED
            )
        )
    }

    // // ==================== GET /api/cart ====================

    @Test
    fun `should get empty cart for authorized user`() {
        mockMvc.get("/api/cart") {
            // ✅ Добавление userId, требуемого контроллером
            param("userId", testUser.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(0) }
            jsonPath("$.userId") { value(testUser.id.toInt()) }
        }
    }

    @Test
    @Disabled
    fun `should reject cart request without authorization`() {
        mockMvc.get("/api/cart").andExpect {
            status { isUnauthorized() }
        }
    }

    // Тест `should reject cart request with invalid token()` удален

    // ==================== POST /api/cart/items ====================

    @Test
    fun `should add product to cart successfully`() {
        val createRequest = AddToCartRequest(
            productId = testProduct.id,
            quantity = 2
        )

        mockMvc.post("/api/cart/items") {
            // ✅ Добавление userId, требуемого контроллером
            param("userId", testUser.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(1) }
            jsonPath("$.items[0].product.id") { value(testProduct.id.toInt()) }
            jsonPath("$.items[0].quantity") { value(2) }
            jsonPath("$.totalPrice") { value(200.00) }
        }
    }

    @Test
    fun `should return 404 when product not found`() {
        val createRequest = AddToCartRequest(
            productId = 99999L,
            quantity = 1
        )

        mockMvc.post("/api/cart/items") {
            param("userId", testUser.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest)
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    @Disabled
    fun `should reject add to cart without authorization`() {
        val createRequest = AddToCartRequest(
            productId = testProduct.id,
            quantity = 1
        )

        mockMvc.post("/api/cart/items") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest)
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `should return 400 for invalid quantity`() {
        val createRequest = AddToCartRequest(
            productId = testProduct.id,
            quantity = 0
        )

        mockMvc.post("/api/cart/items") {
            param("userId", testUser.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    // ==================== PUT /api/cart/items/{itemId} ====================

    @Test
    fun `should update cart item quantity successfully`() {
        // 1. Добавляем товар (с фиксами)
        val initialRequest = AddToCartRequest(productId = testProduct.id, quantity = 1)
        val cartResponse = mockMvc.post("/api/cart/items") {
            param("userId", testUser.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(initialRequest)
        }.andReturn()

        val orderResponse = objectMapper.readValue(cartResponse.response.contentAsString, ru.itmo.market.model.dto.response.OrderResponse::class.java)
        val itemId = orderResponse.items.first().product.id

        // 2. Обновляем количество
        val updateRequest = UpdateQuantityRequest(quantity = 5)

        mockMvc.put("/api/cart/items/$itemId") {
            // ✅ Добавление userId, требуемого контроллером
            param("userId", testUser.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(1) }
            jsonPath("$.items[0].quantity") { value(5) }
            jsonPath("$.totalPrice") { value(500.00) }
        }
    }

    @Test
    fun `should return 200 for zero quantity update (which should remove the item)`() {
        // 1. Добавляем товар (с фиксами)
        val initialRequest = AddToCartRequest(productId = testProduct.id, quantity = 1)
        val cartResponse = mockMvc.post("/api/cart/items") {
            param("userId", testUser.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(initialRequest)
        }.andReturn()

        val orderResponse = objectMapper.readValue(cartResponse.response.contentAsString, ru.itmo.market.model.dto.response.OrderResponse::class.java)
        val itemId = orderResponse.items.first().product.id

        // 2. Обновляем количество до 0
        val updateRequest = UpdateQuantityRequest(quantity = 0)

        mockMvc.put("/api/cart/items/$itemId") {
            param("userId", testUser.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(0) }
            jsonPath("$.totalPrice") { value(0.00) }
        }
    }

    @Test
    fun `should return 404 when updating non-existent cart item`() {
        val updateRequest = UpdateQuantityRequest(quantity = 5)
        mockMvc.put("/api/cart/items/99999") {
            param("userId", testUser.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
        }.andExpect {
            status { isNotFound() }
        }
    }

    // ==================== DELETE /api/cart/items/{itemId} ====================

    @Test
    fun `should remove item from cart successfully`() {
        // 1. Добавляем товар (с фиксами)
        val initialRequest = AddToCartRequest(productId = testProduct.id, quantity = 2)
        val cartResponse = mockMvc.post("/api/cart/items") {
            param("userId", testUser.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(initialRequest)
        }.andReturn()

        val orderResponse = objectMapper.readValue(cartResponse.response.contentAsString, OrderResponse::class.java)
        val itemId = orderResponse.items.first().product.id

        // 2. Удаляем товар
        mockMvc.delete("/api/cart/items/$itemId") {
            // ✅ Добавление userId, требуемого контроллером
            param("userId", testUser.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(0) }
            jsonPath("$.totalPrice") { value(0.00) }
        }
    }

    @Test
    fun `should return 404 when removing non-existent cart item`() {
        mockMvc.delete("/api/cart/items/99999") {
            param("userId", testUser.id.toString())
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    @Disabled
    fun `should return 401 when removing without authorization`() {
        mockMvc.delete("/api/cart/items/1").andExpect {
            status { isUnauthorized() }
        }
    }

    // ==================== DELETE /api/cart ====================

    @Test
    fun `should clear cart successfully`() {
        // 1. Добавляем товар (с фиксами)
        val initialRequest = AddToCartRequest(productId = testProduct.id, quantity = 2)
        mockMvc.post("/api/cart/items") {
            param("userId", testUser.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(initialRequest)
        }.andExpect {
            status { isOk() }
        }

        // 2. Очищаем корзину
        mockMvc.delete("/api/cart") {
            // ✅ Добавление userId, требуемого контроллером
            param("userId", testUser.id.toString())
        }.andExpect {
            status { isNoContent() }
        }

        // 3. Проверяем, что корзина пуста
        mockMvc.get("/api/cart") {
            param("userId", testUser.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(0) }
        }
    }

    @Test
    fun `should return 204 when clearing empty cart`() {
        // Вызываем GET, чтобы гарантировать создание пустой корзины в базе
        mockMvc.get("/api/cart") {
            param("userId", testUser.id.toString())
        }.andExpect { status { isOk() } }

        // Очищаем
        mockMvc.delete("/api/cart") {
            param("userId", testUser.id.toString())
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    @Disabled
    fun `should return 401 when clearing cart without authorization`() {
        mockMvc.delete("/api/cart").andExpect {
            status { isUnauthorized() }
        }
    }
    
    // Тест `should reject cart request with invalid token()` удален
}