package ru.itmo.order.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
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
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.delete
import ru.itmo.order.model.dto.request.AddToCartRequest
import ru.itmo.order.model.dto.request.UpdateQuantityRequest
import ru.itmo.order.model.dto.response.ProductResponse
import ru.itmo.order.repository.OrderItemRepository
import ru.itmo.order.repository.OrderRepository
import ru.itmo.order.service.client.ProductServiceClient
import java.math.BigDecimal
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartControllerIntegrationTest : AbstractIntegrationTest() {

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

    private val userId = 100L
    private val productId = 500L

    @BeforeEach
    fun setUp() {
        orderItemRepository.deleteAll()
        orderRepository.deleteAll()

        // Mock the external Product Service
        val dummyProduct = ProductResponse(
            id = productId, name = "Integration Product", description = "Test",
            price = BigDecimal("150.00"), imageUrl = null, shopId = 1, sellerId = 2,
            status = "APPROVED", rejectionReason = null, averageRating = 0.0, commentsCount = 0,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
        )
        whenever(productServiceClient.getProductById(productId)).thenReturn(dummyProduct)
    }

    @Test
    fun `should add item to cart and calculate total`() {
        val request = AddToCartRequest(productId = productId, quantity = 2)

        mockMvc.post("/api/cart/items") {
            param("userId", userId.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("CART") }
            jsonPath("$.items[0].product.id") { value(productId) }
            jsonPath("$.items[0].quantity") { value(2) }
            jsonPath("$.totalPrice") { value(300.00) } // 150 * 2
        }
    }

    @Test
    fun `should get existing cart`() {
        // First add item to create cart
        orderService_addToCart_helper(2)

        mockMvc.get("/api/cart") {
            param("userId", userId.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(1) }
        }
    }

    @Test
    fun `should update item quantity`() {
        val addedOrder = orderService_addToCart_helper(1)
        val itemId = addedOrder.items[0].id

        val updateRequest = UpdateQuantityRequest(quantity = 5)

        mockMvc.put("/api/cart/items/$itemId") {
            param("userId", userId.toString()) // Usually userId comes from Auth, but controller param is explicit
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.items[0].quantity") { value(5) }
            jsonPath("$.totalPrice") { value(750.00) } // 150 * 5
        }
    }

    @Test
    fun `should clear cart`() {
        orderService_addToCart_helper(1)

        mockMvc.delete("/api/cart") {
            param("userId", userId.toString())
        }.andExpect {
            status { isNoContent() }
        }

        mockMvc.get("/api/cart") {
            param("userId", userId.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(0) }
            jsonPath("$.totalPrice") { value(0) }
        }
    }

    // Helper to simulate service call via controller or direct repo for setup
    private fun orderService_addToCart_helper(qty: Int): ru.itmo.order.model.dto.response.OrderResponse {
        val request = AddToCartRequest(productId = productId, quantity = qty)
        val resString = mockMvc.post("/api/cart/items") {
            param("userId", userId.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andReturn().response.contentAsString
        return objectMapper.readValue(resString, ru.itmo.order.model.dto.response.OrderResponse::class.java)
    }
}