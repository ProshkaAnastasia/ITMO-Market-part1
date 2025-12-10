package ru.itmo.order.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import ru.itmo.order.model.dto.request.AddToCartRequest
import ru.itmo.order.model.dto.request.CreateOrderRequest
import ru.itmo.order.model.dto.response.ProductResponse
import ru.itmo.order.model.enums.OrderStatus
import ru.itmo.order.repository.OrderItemRepository
import ru.itmo.order.repository.OrderRepository
import ru.itmo.order.service.client.ProductServiceClient
import java.math.BigDecimal
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var orderItemRepository: OrderItemRepository

    @MockBean
    private lateinit var productServiceClient: ProductServiceClient

    private val userId = 200L
    private val productId = 600L

    @BeforeEach
    fun setUp() {
        orderItemRepository.deleteAll()
        orderRepository.deleteAll()

        // Mock product service
        val dummyProduct = ProductResponse(
            id = productId, name = "Order Product", description = "Test",
            price = BigDecimal("200.00"), imageUrl = null, shopId = 1, sellerId = 2,
            status = "APPROVED", rejectionReason = null, averageRating = 0.0, commentsCount = 0,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
        )
        whenever(productServiceClient.getProductById(productId)).thenReturn(dummyProduct)
    }

    @Test
    fun `createOrder should succeed when cart is not empty`() {
        // 1. Fill Cart
        val addRequest = AddToCartRequest(productId = productId, quantity = 1)
        mockMvc.post("/api/cart/items") {
            param("userId", userId.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(addRequest)
        }.andExpect { status { isOk() } }

        // 2. Create Order
        val createRequest = CreateOrderRequest(deliveryAddress = "123 Test St")
        mockMvc.post("/api/orders") {
            param("userId", userId.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.status") { value("PENDING") }
            jsonPath("$.deliveryAddress") { value("123 Test St") }
        }

        // 3. Verify Cart is empty (a new cart was created)
        mockMvc.get("/api/cart") {
            param("userId", userId.toString())
        }.andExpect {
            jsonPath("$.items.length()") { value(0) }
            jsonPath("$.status") { value("CART") }
        }
    }

    @Test
    fun `createOrder should fail if cart is empty`() {
        val createRequest = CreateOrderRequest(deliveryAddress = "123 Test St")

        // Ensure a cart exists but is empty
        mockMvc.get("/api/cart") { param("userId", userId.toString()) }

        mockMvc.post("/api/orders") {
            param("userId", userId.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `getUserOrders should return list of orders`() {
        // Create an order first
        `createOrder should succeed when cart is not empty`()

        mockMvc.get("/api/orders") {
            param("userId", userId.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.length()") { value(1) }
            jsonPath("$.data[0].status") { value("PENDING") }
        }
    }
}