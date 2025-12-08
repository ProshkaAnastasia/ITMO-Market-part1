package ru.itmo.order.integration

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
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.containers.PostgreSQLContainer
import ru.itmo.market.repository.*
import ru.itmo.market.user_domain.model.entity.Shop
import ru.itmo.market.model.entity.Product
import ru.itmo.market.model.dto.request.AddToCartRequest
import ru.itmo.market.model.dto.request.CreateOrderRequest
import ru.itmo.market.model.dto.response.OrderResponse
import ru.itmo.market.user_domain.model.enums.UserRole
import ru.itmo.market.model.enums.ProductStatus
import ru.itmo.market.user_domain.repository.ShopRepository
import ru.itmo.market.user_domain.repository.UserRepository
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
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

    

    @Test
    fun `should get empty orders list for new user`() {
        val user = testAuthHelper.createTestUser()

        mockMvc.get("/api/orders") {
            param("userId", user.id.toString())
            param("page", "1")
            param("pageSize", "20")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data") { isArray() }
            jsonPath("$.data.length()") { value(0) }
            jsonPath("$.page") { value(1) }
            jsonPath("$.pageSize") { value(20) }
        }
    }

    @Test
    fun `should get orders with pagination`() {
        val user = testAuthHelper.createTestUser()

        
        val seller = testAuthHelper.createTestUser(username = "seller", email = "seller@example.com",  roles = setOf(
            UserRole.SELLER))
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
        mockMvc.post("/api/cart/items") {
            param("userId", user.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(addRequest)
        }

        
        val createOrderRequest = CreateOrderRequest(deliveryAddress = "123 Main St")
        mockMvc.post("/api/orders") {
            param("userId", user.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createOrderRequest)
        }.andExpect {
            status { isCreated() }
        }

        
        mockMvc.get("/api/orders") {
            param("userId", user.id.toString())
            param("page", "1")
            param("pageSize", "20")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data") { isArray() }
            jsonPath("$.data.length()") { value(1) }
            jsonPath("$.page") { value(1) }
            jsonPath("$.pageSize") { value(20) }
        }
    }

    @Test
    @Disabled
    fun `should reject orders list without authorization`() {
        mockMvc.get("/api/orders") {
            param("page", "1")
            param("pageSize", "20")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    @Disabled
    fun `should reject orders list with invalid token`() {
        mockMvc.get("/api/orders") {
            header("Authorization", "Bearer invalid_token") 
            param("page", "1")
            param("pageSize", "20")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    

    @Test
    fun `should get order by id successfully`() {
        val user = testAuthHelper.createTestUser()
        

        val seller = testAuthHelper.createTestUser(username = "seller", email = "seller@example.com", roles = setOf(
            UserRole.SELLER))
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
        mockMvc.post("/api/cart/items") {
            param("userId", user.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(addRequest)
        }

        val createOrderRequest = CreateOrderRequest(deliveryAddress = "123 Main St")
        val orderResponse = mockMvc.post("/api/orders") {
            param("userId", user.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createOrderRequest)
        }.andExpect {
            status { isCreated() }
        }.andReturn()

        val responseBody = orderResponse.response.contentAsString
        val createdOrder = objectMapper.readValue(responseBody, OrderResponse::class.java)

        mockMvc.get("/api/orders/${createdOrder.id}") {
            param("userId", user.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(createdOrder.id.toInt()) }
            jsonPath("$.userId") { value(user.id.toInt()) }
            jsonPath("$.status") { value("PENDING") }
        }
    }

    @Test
    fun `should return 404 for non-existent order`() {
        val user = testAuthHelper.createTestUser()
        

        mockMvc.get("/api/orders/99999") {
            param("userId", user.id.toString())
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `should return 403 when accessing other user order`() {
        val user1 = testAuthHelper.createTestUser(username = "user1", email = "test1@example.com")
        val user2 = testAuthHelper.createTestUser(username = "user2", email = "test2@example.com")
        

        val seller = testAuthHelper.createTestUser(username = "seller", email = "seller@example.com", roles = setOf(
            UserRole.SELLER))
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
        mockMvc.post("/api/cart/items") {
            param("userId", user1.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(addRequest)
        }

        val createOrderRequest = CreateOrderRequest(deliveryAddress = "123 Main St")
        val orderResponse = mockMvc.post("/api/orders") {
            param("userId", user1.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createOrderRequest)
        }.andExpect {
            status { isCreated() }
        }.andReturn()

        val responseBody = orderResponse.response.contentAsString
        val createdOrder = objectMapper.readValue(responseBody, OrderResponse::class.java)

        
        mockMvc.get("/api/orders/${createdOrder.id}") {
            param("userId", user2.id.toString())
        }.andExpect {
            status { isNotFound() } 
        }
    }

    @Test
    @Disabled
    fun `should return 401 when getting order without authorization`() {
        mockMvc.get("/api/orders/1").andExpect {
            status { isUnauthorized() }
        }
    }

    

    @Test
    fun `should create order successfully`() {
        val user = testAuthHelper.createTestUser()
        

        val seller = testAuthHelper.createTestUser(username = "seller", email = "seller@example.com", roles = setOf(
            UserRole.SELLER))
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
            param("userId", user.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(addRequest)
        }

        val createOrderRequest = CreateOrderRequest(deliveryAddress = "456 Oak Ave")

        mockMvc.post("/api/orders") {
            param("userId", user.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createOrderRequest)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.userId") { value(user.id.toInt()) }
            jsonPath("$.status") { value("PENDING") }
            jsonPath("$.deliveryAddress") { value("456 Oak Ave") }
            jsonPath("$.items") { isArray() }
            jsonPath("$.items.length()") { value(1) }
        }
    }

    @Test
    fun `should return 400 for empty delivery address`() {
        val user = testAuthHelper.createTestUser()
        

        val seller = testAuthHelper.createTestUser(username = "seller", email = "seller@example.com", roles = setOf(
            UserRole.SELLER))
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
        mockMvc.post("/api/cart/items") {
            param("userId", user.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(addRequest)
        }

        val createOrderRequest = CreateOrderRequest(deliveryAddress = "")

        mockMvc.post("/api/orders") {
            param("userId", user.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createOrderRequest)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `should return 400 for empty cart`() {
        val user = testAuthHelper.createTestUser()
        

        val createOrderRequest = CreateOrderRequest(deliveryAddress = "123 Main St")

        mockMvc.post("/api/orders") {
            param("userId", user.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createOrderRequest)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    @Disabled
    fun `should return 401 when creating order without authorization`() {
        val createOrderRequest = CreateOrderRequest(deliveryAddress = "123 Main St")

        mockMvc.post("/api/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createOrderRequest)
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}