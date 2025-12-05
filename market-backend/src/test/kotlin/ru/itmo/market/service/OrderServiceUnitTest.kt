package ru.itmo.market.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import ru.itmo.market.exception.BadRequestException
import ru.itmo.market.exception.ResourceNotFoundException
import ru.itmo.market.model.dto.response.ProductResponse
import ru.itmo.market.model.entity.Order
import ru.itmo.market.model.entity.OrderItem
import ru.itmo.market.model.enums.OrderStatus
import ru.itmo.market.model.enums.ProductStatus
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
    private lateinit var productService: ProductService

    private lateinit var orderService: OrderService

    private val USER_ID = 1L
    private val PRODUCT_ID = 100L

    @BeforeEach
    fun setUp() {
        orderService = OrderService(
            orderRepository,
            orderItemRepository,
            productService
        )
    }

    // Helper для создания DTO продукта (так как OrderService работает с DTO)
    private fun createProductResponse(productId: Long, price: BigDecimal): ProductResponse {
        return ProductResponse(
            id = productId,
            name = "Test Product",
            description = "Desc",
            price = price,
            imageUrl = null,
            shopId = 1L,
            sellerId = 2L,
            status = ProductStatus.APPROVED.name,
            rejectionReason = null,
            averageRating = null,
            commentsCount = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    @Test
    fun `addToCart should add new item and recalculate total`() {
        val quantity = 2
        val price = BigDecimal("50.00")
        
        // 1. Mock Cart
        val cart = Order(id = 1L, userId = USER_ID, status = OrderStatus.CART, totalPrice = BigDecimal.ZERO)
        whenever(orderRepository.findByUserIdAndStatus(eq(USER_ID), eq(OrderStatus.CART)))
            .thenReturn(Optional.of(cart))

        // 2. Mock ProductService (возвращает DTO)
        val productDto = createProductResponse(PRODUCT_ID, price)
        whenever(productService.getProductById(eq(PRODUCT_ID))).thenReturn(productDto)

        // 3. Mock OrderItem lookup (пусто, т.к. товар новый)
        whenever(orderItemRepository.findByOrderIdAndProductId(eq(cart.id), eq(PRODUCT_ID)))
            .thenReturn(Optional.empty())

        // 4. Mock OrderItem saving
        whenever(orderItemRepository.save(any<OrderItem>())).thenAnswer { 
            (it.arguments[0] as OrderItem).copy(id = 10L) 
        }

        // 5. Mock recalculation (возвращаем список товаров для пересчета)
        val newItem = OrderItem(orderId = cart.id, productId = PRODUCT_ID, quantity = quantity, price = price)
        whenever(orderItemRepository.findAllByOrderId(eq(cart.id))).thenReturn(listOf(newItem))

        // 6. Mock Cart saving
        whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] as Order }

        // Act
        val result = orderService.addToCart(USER_ID, PRODUCT_ID, quantity)

        // Assert
        assertEquals(BigDecimal("100.00"), result.totalPrice) // 50 * 2
        verify(productService, atLeastOnce()).getProductById(PRODUCT_ID)
        verify(orderItemRepository).save(argThat<OrderItem> { 
            productId == PRODUCT_ID && quantity == 2 
        })
    }

    @Test
    fun `addToCart should increment quantity for existing item`() {
        val quantityToAdd = 1
        val price = BigDecimal("50.00")
        
        val cart = Order(id = 1L, userId = USER_ID, status = OrderStatus.CART)
        whenever(orderRepository.findByUserIdAndStatus(USER_ID, OrderStatus.CART)).thenReturn(Optional.of(cart))
        
        // ProductService нужен для получения цены, даже если item существует (в вашей логике)
        val productDto = createProductResponse(PRODUCT_ID, price)
        whenever(productService.getProductById(PRODUCT_ID)).thenReturn(productDto)

        // Существующий item
        val existingItem = OrderItem(id = 10L, orderId = cart.id, productId = PRODUCT_ID, quantity = 1, price = price)
        whenever(orderItemRepository.findByOrderIdAndProductId(cart.id, PRODUCT_ID))
            .thenReturn(Optional.of(existingItem))

        // Recalculation logic
        val updatedItem = existingItem.copy(quantity = 2)
        whenever(orderItemRepository.findAllByOrderId(cart.id)).thenReturn(listOf(updatedItem))
        whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] as Order }

        // Act
        val result = orderService.addToCart(USER_ID, PRODUCT_ID, quantityToAdd)

        // Assert
        assertEquals(BigDecimal("100.00"), result.totalPrice) // 50 * 2
        verify(orderItemRepository).save(check {
            assertEquals(2, it.quantity)
        })
    }

    @Test
    fun `createOrder should convert cart to pending and create new cart`() {
        val address = "Main St"
        val cart = Order(id = 1L, userId = USER_ID, status = OrderStatus.CART, totalPrice = BigDecimal("100.00"))
        
        whenever(orderRepository.findByUserIdAndStatus(USER_ID, OrderStatus.CART)).thenReturn(Optional.of(cart))
        
        // Корзина не пуста
        whenever(orderItemRepository.findAllByOrderId(cart.id))
            .thenReturn(listOf(OrderItem(id=1, orderId=1, productId=PRODUCT_ID, quantity=1, price=BigDecimal("100"))))

        // Сохранение заказа (Pending)
        whenever(orderRepository.save(argThat<Order> { status == OrderStatus.PENDING }))
            .thenAnswer { it.arguments[0] as Order }
            
        // Product lookup for toResponse mapping
        val productDto = createProductResponse(PRODUCT_ID, BigDecimal("100.00"))
        whenever(productService.getProductById(PRODUCT_ID)).thenReturn(productDto)

        // Act
        val result = orderService.createOrder(USER_ID, address)

        // Assert
        assertEquals(OrderStatus.PENDING.name, result.status)
        assertEquals(address, result.deliveryAddress)
        
        // Проверяем, что создалась новая корзина
        verify(orderRepository).save(argThat<Order> { 
            status == OrderStatus.CART && totalPrice == BigDecimal.ZERO 
        })
    }

    @Test
    fun `createOrder should throw exception if cart is empty`() {
        val cart = Order(id = 1L, userId = USER_ID, status = OrderStatus.CART)
        whenever(orderRepository.findByUserIdAndStatus(USER_ID, OrderStatus.CART)).thenReturn(Optional.of(cart))
        whenever(orderItemRepository.findAllByOrderId(cart.id)).thenReturn(emptyList())

        assertThrows<BadRequestException> {
            orderService.createOrder(USER_ID, "addr")
        }
        
        // Не должен сохранять заказ или создавать новую корзину
        verify(orderRepository, never()).save(any())
    }
}