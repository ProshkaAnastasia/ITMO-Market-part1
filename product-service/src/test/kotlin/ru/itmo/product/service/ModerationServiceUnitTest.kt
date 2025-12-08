package ru.itmo.product.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import ru.itmo.market.exception.ResourceNotFoundException
import ru.itmo.market.model.dto.response.PaginatedResponse
import ru.itmo.market.model.dto.response.ProductResponse
import ru.itmo.market.model.dto.response.UserResponse
import ru.itmo.market.model.enums.ProductStatus
import java.math.BigDecimal
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class ModerationServiceUnitTest {

    
    @Mock
    private lateinit var productService: ProductService

    @Mock
    private lateinit var userService: UserService

    private lateinit var moderationService: ModerationService

    
    private val PRODUCT_ID = 100L
    private val MODERATOR_ID = 50L
    private val REJECT_REASON = "Violation of rules"
    private val SHOP_ID = 10L
    private val SELLER_ID = 20L

    @BeforeEach
    fun setUp() {

        moderationService = ModerationService(
            productService,
            userService
        )

    }

    
    private fun createProductResponse(status: ProductStatus, rejectionReason: String? = null): ProductResponse {
        return ProductResponse(
            id = PRODUCT_ID,
            name = "Test Item",
            description = "Desc",
            price = BigDecimal("50.00"),
            imageUrl = "http://img.com/1",
            shopId = SHOP_ID,
            sellerId = SELLER_ID,
            status = status.name,
            rejectionReason = rejectionReason,
            averageRating = 4.5, 
            commentsCount = 12L, 
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    
    
    

    @Test
    fun `approveProduct should succeed and delegate to productService`() {
        val approvedResponse = createProductResponse(ProductStatus.APPROVED)
        
        
        whenever(productService.approveProduct(eq(PRODUCT_ID), eq(MODERATOR_ID)))
            .thenReturn(approvedResponse)

        val result = moderationService.approveProduct(PRODUCT_ID, MODERATOR_ID)

        
        assertEquals(ProductStatus.APPROVED.name, result.status)

        
        verify(productService, times(1)).approveProduct(PRODUCT_ID, MODERATOR_ID)
    }

    @Test
    fun `approveProduct should throw exception when product not found (delegated)`() {
        
        whenever(productService.approveProduct(eq(PRODUCT_ID), eq(MODERATOR_ID)))
            .thenThrow(ResourceNotFoundException("Товар не найден"))

        assertThrows<ResourceNotFoundException> {
            moderationService.approveProduct(PRODUCT_ID, MODERATOR_ID)
        }

        verify(productService, times(1)).approveProduct(PRODUCT_ID, MODERATOR_ID)
    }

    @Test
    fun `approveProduct should throw exception when status is not PENDING (delegated)`() {
        
        whenever(productService.approveProduct(eq(PRODUCT_ID), eq(MODERATOR_ID)))
            .thenThrow(IllegalStateException("Может быть одобрен только товар со статусом PENDING"))

        val ex = assertThrows<IllegalStateException> {
            moderationService.approveProduct(PRODUCT_ID, MODERATOR_ID)
        }
        
        assertEquals("Может быть одобрен только товар со статусом PENDING", ex.message)
        verify(productService, times(1)).approveProduct(PRODUCT_ID, MODERATOR_ID)
    }

    
    
    

    @Test
    fun `rejectProduct should succeed and delegate to productService`() {
        val rejectedResponse = createProductResponse(ProductStatus.REJECTED, REJECT_REASON)

        
        whenever(productService.rejectProduct(eq(PRODUCT_ID), eq(MODERATOR_ID), eq(REJECT_REASON)))
            .thenReturn(rejectedResponse)

        val result = moderationService.rejectProduct(PRODUCT_ID, MODERATOR_ID, REJECT_REASON)

        assertEquals(ProductStatus.REJECTED.name, result.status)
        assertEquals(REJECT_REASON, result.rejectionReason)

        verify(productService, times(1)).rejectProduct(PRODUCT_ID, MODERATOR_ID, REJECT_REASON)
    }

    @Test
    fun `rejectProduct should throw exception when status is not PENDING (delegated)`() {
        
        whenever(productService.rejectProduct(eq(PRODUCT_ID), eq(MODERATOR_ID), eq(REJECT_REASON)))
            .thenThrow(IllegalStateException("Может быть отклонен только товар со статусом PENDING"))

        val ex = assertThrows<IllegalStateException> {
            moderationService.rejectProduct(PRODUCT_ID, MODERATOR_ID, REJECT_REASON)
        }
        
        assertEquals("Может быть отклонен только товар со статусом PENDING", ex.message)
        verify(productService, times(1)).rejectProduct(PRODUCT_ID, MODERATOR_ID, REJECT_REASON)
    }

    @Test
    fun `rejectProduct should throw exception when product not found (delegated)`() {
        
        whenever(productService.rejectProduct(eq(PRODUCT_ID), eq(MODERATOR_ID), eq(REJECT_REASON)))
            .thenThrow(ResourceNotFoundException("Товар не найден"))

        assertThrows<ResourceNotFoundException> {
            moderationService.rejectProduct(PRODUCT_ID, MODERATOR_ID, REJECT_REASON)
        }
        verify(productService, times(1)).rejectProduct(PRODUCT_ID, MODERATOR_ID, REJECT_REASON)
    }

    
    
    

    @Test
    fun `getPendingProducts should return populated page (delegated)`() {
        val page = 1
        val pageSize = 10
        val pendingResponse = createProductResponse(ProductStatus.PENDING)
        
        val expectedResponse = PaginatedResponse(
            data = listOf(pendingResponse),
            page = page,
            pageSize = pageSize,
            totalElements = 1L,
            totalPages = 1
        )

        
        whenever(productService.getPendingProducts(eq(page), eq(pageSize)))
            .thenReturn(expectedResponse)

        whenever(userService.getUserById(eq(MODERATOR_ID)))
            .thenReturn(UserResponse(id = MODERATOR_ID,
                username = "moderator",
                email = "testmod@example.com",
                firstName = "Mod",
                lastName = "Test",
                roles = setOf("MODERATOR"),
                createdAt = LocalDateTime.now()))

        val result = moderationService.getPendingProducts(MODERATOR_ID, page, pageSize)

        assertEquals(1, result.data.size)
        assertEquals(pendingResponse.averageRating, result.data[0].averageRating)
        
        
        verify(productService, times(1)).getPendingProducts(page, pageSize)
        
    }

    @Test
    fun `getPendingProducts should handle empty result (delegated)`() {
        val page = 1
        val pageSize = 10
        
        val emptyResponse = PaginatedResponse<ProductResponse>(
            data = emptyList(),
            page = page,
            pageSize = pageSize,
            totalElements = 0L,
            totalPages = 0
        )
        
        
        whenever(productService.getPendingProducts(eq(page), eq(pageSize)))
            .thenReturn(emptyResponse)

        whenever(userService.getUserById(eq(MODERATOR_ID)))
            .thenReturn(UserResponse(id = MODERATOR_ID,
                username = "moderator",
                email = "testmod@example.com",
                firstName = "Mod",
                lastName = "Test",
                roles = setOf("MODERATOR"),
                createdAt = LocalDateTime.now()))

        val result = moderationService.getPendingProducts(MODERATOR_ID, page, pageSize)

        assertTrue(result.data.isEmpty())
        verify(productService, times(1)).getPendingProducts(page, pageSize)
    }

    
    
    

    @Test
    fun `getPendingProductById should return details when status is PENDING (delegated)`() {
        val pendingResponse = createProductResponse(ProductStatus.PENDING)
        
        
        whenever(productService.getPendingProductById(eq(PRODUCT_ID)))
            .thenReturn(pendingResponse)

        whenever(userService.getUserById(eq(MODERATOR_ID)))
            .thenReturn(UserResponse(id = MODERATOR_ID,
                username = "moderator",
                email = "testmod@example.com",
                firstName = "Mod",
                lastName = "Test",
                roles = setOf("MODERATOR"),
                createdAt = LocalDateTime.now()))

        val result = moderationService.getPendingProductById(MODERATOR_ID, PRODUCT_ID)

        assertEquals(PRODUCT_ID, result.id)
        assertEquals(ProductStatus.PENDING.name, result.status)
        assertEquals(pendingResponse.averageRating, result.averageRating)
        
        verify(productService, times(1)).getPendingProductById(PRODUCT_ID)
    }

    @Test
    fun `getPendingProductById should throw exception when status is NOT PENDING (delegated)`() {
        
        whenever(productService.getPendingProductById(eq(PRODUCT_ID)))
            .thenThrow(ResourceNotFoundException("Товар не на модерации"))

        whenever(userService.getUserById(eq(MODERATOR_ID)))
            .thenReturn(UserResponse(id = MODERATOR_ID,
                username = "moderator",
                email = "testmod@example.com",
                firstName = "Mod",
                lastName = "Test",
                roles = setOf("MODERATOR"),
                createdAt = LocalDateTime.now()))

        val ex = assertThrows<ResourceNotFoundException> {
            moderationService.getPendingProductById(MODERATOR_ID, PRODUCT_ID)
        }
        
        assertEquals("Товар не на модерации", ex.message)
        verify(productService, times(1)).getPendingProductById(PRODUCT_ID)
    }

    @Test
    fun `getPendingProductById should throw exception when not found (delegated)`() {
        
        whenever(productService.getPendingProductById(eq(PRODUCT_ID)))
            .thenThrow(ResourceNotFoundException("Товар не найден"))

        whenever(userService.getUserById(eq(MODERATOR_ID)))
            .thenReturn(UserResponse(id = MODERATOR_ID,
                username = "moderator",
                email = "testmod@example.com",
                firstName = "Mod",
                lastName = "Test",
                roles = setOf("MODERATOR"),
                createdAt = LocalDateTime.now()))

        assertThrows<ResourceNotFoundException> {
            moderationService.getPendingProductById(MODERATOR_ID, PRODUCT_ID)
        }
        verify(productService, times(1)).getPendingProductById(PRODUCT_ID)
    }
}