package ru.itmo.market.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import ru.itmo.market.exception.ResourceNotFoundException
import ru.itmo.market.model.entity.Order
import ru.itmo.market.model.entity.OrderItem
import ru.itmo.market.model.entity.Product
import ru.itmo.market.model.enums.OrderStatus
import ru.itmo.market.model.enums.ProductStatus
import ru.itmo.market.repository.OrderRepository
import ru.itmo.market.repository.OrderItemRepository
import ru.itmo.market.repository.ProductRepository
import ru.itmo.market.repository.CommentRepository
import java.time.LocalDateTime
import java.util.*
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
class OrderServiceUnitTest {

    @Mock
    private lateinit var orderRepository: OrderRepository
    
    @Mock
    private lateinit var orderItemRepository: OrderItemRepository
    
    @Mock
    private lateinit var productRepository: ProductRepository
    
    @Mock
    private lateinit var commentRepository: CommentRepository

    private lateinit var orderService: OrderService

    @BeforeEach
    fun setUp() {
        orderService = OrderService(
            orderRepository,
            orderItemRepository,
            productRepository,
            commentRepository
        )
    }

    @Test
    fun `should add item to cart successfully`() {
        val userId = 1L
        val productId = 100L
        val quantity = 2

        val cart = Order(
            id = 1L,
            userId = userId,
            status = OrderStatus.PENDING,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        whenever(orderRepository.findByUserIdAndStatus(eq(userId), any()))
            .thenReturn(Optional.of(cart))

        val product = Product(
            id = productId,
            name = "Test Product",
            description = "Test Description",
            price = BigDecimal("99.99"),
            shopId = 1L,
            sellerId = 1L,
            status = ProductStatus.APPROVED,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        whenever(productRepository.findById(eq(productId)))
            .thenReturn(Optional.of(product))

        whenever(orderItemRepository.findByOrderIdAndProductId(eq(cart.id), eq(productId)))
            .thenReturn(Optional.empty())

        val orderItem = OrderItem(
            id = 1L,
            orderId = cart.id,
            productId = productId,
            quantity = quantity,
            price = product.price
        )
        
        val orderItemCaptor = argumentCaptor<OrderItem>()
        whenever(orderItemRepository.save(orderItemCaptor.capture())).thenAnswer { invocation ->
            val item = invocation.arguments[0] as OrderItem
            item.copy(id = 1L)
        }
        
        whenever(orderItemRepository.findAllByOrderId(eq(cart.id)))
            .thenReturn(listOf(orderItem))
        
        val orderCaptor = argumentCaptor<Order>()
        whenever(orderRepository.save(orderCaptor.capture())).thenAnswer { invocation ->
            val order = invocation.arguments[0] as Order
            order.copy(id = cart.id)
        }

        val result = orderService.addToCart(userId, productId, quantity)

        assertNotNull(result)
        assertEquals(userId, result.userId)
        verify(orderItemRepository, times(1)).save(any())
        verify(orderRepository, times(1)).save(any())
    }

    @Test
    fun `should add quantity to existing cart item`() {
        val userId = 1L
        val productId = 100L
        val quantity = 2

        val cart = Order(
            id = 1L,
            userId = userId,
            status = OrderStatus.PENDING,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        whenever(orderRepository.findByUserIdAndStatus(eq(userId), any()))
            .thenReturn(Optional.of(cart))

        val product = Product(
            id = productId,
            name = "Test Product",
            description = "Test Description",
            price = BigDecimal("99.99"),
            shopId = 1L,
            sellerId = 1L,
            status = ProductStatus.APPROVED,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        whenever(productRepository.findById(eq(productId)))
            .thenReturn(Optional.of(product))

        val existingOrderItem = OrderItem(
            id = 1L,
            orderId = cart.id,
            productId = productId,
            quantity = 1,
            price = product.price
        )
        
        whenever(orderItemRepository.findByOrderIdAndProductId(eq(cart.id), eq(productId)))
            .thenReturn(Optional.of(existingOrderItem))

        val orderItemCaptor = argumentCaptor<OrderItem>()
        whenever(orderItemRepository.save(orderItemCaptor.capture())).thenAnswer { invocation ->
            val item = invocation.arguments[0] as OrderItem
            item.copy(id = 1L, quantity = 3)
        }
        
        whenever(orderItemRepository.findAllByOrderId(eq(cart.id)))
            .thenReturn(listOf(existingOrderItem.copy(quantity = 3)))
        
        val orderCaptor = argumentCaptor<Order>()
        whenever(orderRepository.save(orderCaptor.capture())).thenAnswer { invocation ->
            val order = invocation.arguments[0] as Order
            order.copy(id = cart.id)
        }

        val result = orderService.addToCart(userId, productId, quantity)

        assertNotNull(result)
        verify(orderItemRepository, times(1)).save(any())
    }

    @Test
    fun `should throw exception when product not found`() {
        val userId = 1L
        val productId = 999L
        val quantity = 2

        val cart = Order(
            id = 1L,
            userId = userId,
            status = OrderStatus.PENDING,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        whenever(orderRepository.findByUserIdAndStatus(eq(userId), any()))
            .thenReturn(Optional.of(cart))

        whenever(productRepository.findById(eq(productId)))
            .thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            orderService.addToCart(userId, productId, quantity)
        }

        verify(orderItemRepository, never()).save(any())
    }

    @Test
    fun `should throw exception when cart not found`() {
        val userId = 999L
        val productId = 100L
        val quantity = 2

        whenever(orderRepository.findByUserIdAndStatus(eq(userId), any()))
            .thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            orderService.addToCart(userId, productId, quantity)
        }

        verify(orderRepository, times(1)).findByUserIdAndStatus(eq(userId), any())
    }

}
