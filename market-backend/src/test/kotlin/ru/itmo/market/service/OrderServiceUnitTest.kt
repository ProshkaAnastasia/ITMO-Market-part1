package ru.itmo.market.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import ru.itmo.market.model.dto.response.ProductResponse
import ru.itmo.market.model.entity.Order
import ru.itmo.market.model.entity.OrderItem
import ru.itmo.market.model.enums.OrderStatus
import ru.itmo.market.repository.OrderItemRepository
import ru.itmo.market.repository.OrderRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class OrderServiceUnitTest {

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var orderItemRepository: OrderItemRepository

    @Mock
    private lateinit var productService: ProductService // ✅ FIXED: Service, not Repo

    private lateinit var orderService: OrderService

    @BeforeEach
    fun setUp() {
        orderService = OrderService(
            orderRepository,
            orderItemRepository,
            productService // ✅ FIXED: Correct Constructor
        )
    }

    // @Test
    // fun `addToCart should create item if not exists`() {
    //     val userId = 1L
    //     val productId = 100L
    //     val quantity = 2
    //     val price = BigDecimal("50.00")

    //     val cart = Order(id = 1L, userId = userId, status = OrderStatus.CART, totalPrice = BigDecimal.ZERO)
    //     whenever(orderRepository.findByUserIdAndStatus(userId, OrderStatus.CART))
    //         .thenReturn(Optional.of(cart))

    //     // Mock ProductService to return DTO
    //     val productResponse = ProductResponse(
    //         id = productId, name = "Prod", description = "Desc", price = price,
    //         imageUrl = null, shopId = 1L, sellerId = 1L, status = "APPROVED",
    //         rejectionReason = null, averageRating = null, commentsCount = null,
    //         createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
    //     )
    //     whenever(productService.getProductById(productId)).thenReturn(productResponse)

    //     whenever(orderItemRepository.findByOrderIdAndProductId(cart.id, productId))
    //         .thenReturn(Optional.empty())

    //     // Act
    //     orderService.addToCart(userId, productId, quantity)

    //     // Verify
    //     verify(orderItemRepository).save(check {
    //         assertEquals(quantity, it.quantity)
    //         assertEquals(price, it.price)
    //     })
    // }
}