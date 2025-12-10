package ru.itmo.order.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import ru.itmo.order.exception.BadRequestException
import ru.itmo.order.exception.ResourceNotFoundException
import ru.itmo.order.model.dto.response.ProductResponse
import ru.itmo.order.model.entity.Order
import ru.itmo.order.model.entity.OrderItem
import ru.itmo.order.model.enums.OrderStatus
import ru.itmo.order.repository.OrderItemRepository
import ru.itmo.order.repository.OrderRepository
import ru.itmo.order.service.client.ProductServiceClient
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
    private lateinit var productServiceClient: ProductServiceClient

    @InjectMocks
    private lateinit var orderService: OrderService

    private val userId = 1L
    private val productId = 100L
    private val cartId = 10L

    @Test
    fun `addToCart should create new cart and add item if cart missing`() {
        // Arrange
        val productResponse = ProductResponse(
            id = productId, name = "Test Product", description = "Desc",
            price = BigDecimal("50.00"), imageUrl = null, shopId = 1, sellerId = 2,
            status = "APPROVED", rejectionReason = null, averageRating = 5.0, commentsCount = 0,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
        )

        whenever(orderRepository.findByUserIdAndStatus(userId, OrderStatus.CART))
            .thenReturn(Optional.empty()) // Cart doesn't exist

        whenever(orderRepository.save(any<Order>())).thenAnswer {
            (it.arguments[0] as Order).copy(id = cartId) // Return cart with ID
        }

        whenever(productServiceClient.getProductById(productId)).thenReturn(productResponse)
        whenever(orderItemRepository.findByOrderIdAndProductId(cartId, productId))
            .thenReturn(Optional.empty()) // Item not in cart

        whenever(orderItemRepository.findAllByOrderId(cartId)).thenReturn(
            listOf(OrderItem(orderId = cartId, productId = productId, quantity = 2, price = BigDecimal("50.00")))
        )

        // Act
        val result = orderService.addToCart(userId, productId, 2)

        // Assert
        verify(orderRepository, times(2)).save(any()) // 1 for new cart, 1 for update total
        verify(orderItemRepository).save(any())
        assertEquals(BigDecimal("100.00"), result.totalPrice)
    }

    @Test
    fun `createOrder should convert cart to pending order and create new cart`() {
        // Arrange
        val cart = Order(id = cartId, userId = userId, status = OrderStatus.CART, totalPrice = BigDecimal("100.00"))
        
        whenever(orderRepository.findByUserIdAndStatus(userId, OrderStatus.CART))
            .thenReturn(Optional.of(cart))
        
        whenever(orderItemRepository.findAllByOrderId(cartId))
            .thenReturn(listOf(OrderItem(orderId = cartId, productId = productId, quantity = 1, price = BigDecimal("100.00"))))

        whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] as Order }

        // Act
        val result = orderService.createOrder(userId, "Test Address")

        // Assert
        assertEquals(OrderStatus.PENDING.name, result.status)
        assertEquals("Test Address", result.deliveryAddress)
        
        // Verify a new empty cart was created
        verify(orderRepository).save(check {
            assertEquals(OrderStatus.CART, it.status)
            assertEquals(BigDecimal.ZERO, it.totalPrice)
        })
    }

    @Test
    fun `createOrder should throw exception if cart is empty`() {
        val cart = Order(id = cartId, userId = userId, status = OrderStatus.CART)
        whenever(orderRepository.findByUserIdAndStatus(userId, OrderStatus.CART)).thenReturn(Optional.of(cart))
        whenever(orderItemRepository.findAllByOrderId(cartId)).thenReturn(emptyList())

        assertThrows<BadRequestException> {
            orderService.createOrder(userId, "Address")
        }
    }

    @Test
    fun `updateCartItemQuantity should delete item if quantity is 0`() {
        val cart = Order(id = cartId, userId = userId, status = OrderStatus.CART)
        val item = OrderItem(id = 5L, orderId = cartId, productId = productId, quantity = 1, price = BigDecimal("10.00"))

        whenever(orderRepository.findByUserIdAndStatus(userId, OrderStatus.CART)).thenReturn(Optional.of(cart))
        whenever(orderItemRepository.findByOrderIdAndProductId(cartId, item.id)).thenReturn(Optional.of(item))
        whenever(orderItemRepository.findAllByOrderId(cartId)).thenReturn(emptyList()) // Cart becomes empty
        whenever(orderRepository.save(any())).thenAnswer { it.arguments[0] }

        // Act
        val result = orderService.updateCartItemQuantity(userId, item.id, 0)

        // Assert
        verify(orderItemRepository).deleteById(item.id)
        assertEquals(BigDecimal.ZERO, result.totalPrice)
    }
}