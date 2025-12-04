package ru.itmo.market.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import ru.itmo.market.exception.ResourceNotFoundException
import ru.itmo.market.model.entity.Product
import ru.itmo.market.model.enums.ProductStatus
import ru.itmo.market.repository.CommentRepository
import ru.itmo.market.repository.ProductRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class ModerationServiceUnitTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var commentRepository: CommentRepository

    private lateinit var moderationService: ModerationService

    // Test Data Constants
    private val PRODUCT_ID = 100L
    private val MODERATOR_ID = 50L
    private val REJECT_REASON = "Violation of rules"
    private val SHOP_ID = 10L
    private val SELLER_ID = 20L

    @BeforeEach
    fun setUp() {
        moderationService = ModerationService(
            productRepository,
            commentRepository
        )
    }

    // ==========================================
    // 1. Tests for approvProduct
    // ==========================================

    @Test
    fun `approvProduct should succeed when product is PENDING`() {
        val pendingProduct = createProduct(ProductStatus.PENDING)
        
        // Mock finding the product
        whenever(productRepository.findById(eq(PRODUCT_ID)))
            .thenReturn(Optional.of(pendingProduct))

        // Mock saving the product (capture arguments to verify state change)
        val productCaptor = argumentCaptor<Product>()
        whenever(productRepository.save(productCaptor.capture())).thenAnswer { 
            it.arguments[0] as Product // Simulate save returning the object
        }

        val result = moderationService.approveProduct(PRODUCT_ID, MODERATOR_ID)

        // Assert Response
        assertEquals(ProductStatus.APPROVED.name, result.status)
        assertNull(result.rejectionReason)

        // Assert Persistence State
        val capturedProduct = productCaptor.firstValue
        assertEquals(ProductStatus.APPROVED, capturedProduct.status)
        assertNull(capturedProduct.rejectionReason)
        
        verify(productRepository).save(any())
    }

    @Test
    fun `approvProduct should throw exception when product not found`() {
        whenever(productRepository.findById(eq(PRODUCT_ID)))
            .thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            moderationService.approveProduct(PRODUCT_ID, MODERATOR_ID)
        }

        verify(productRepository, never()).save(any())
    }

    @Test
    fun `approvProduct should throw exception when status is not PENDING`() {
        val approvedProduct = createProduct(ProductStatus.APPROVED)
        
        whenever(productRepository.findById(eq(PRODUCT_ID)))
            .thenReturn(Optional.of(approvedProduct))

        val ex = assertThrows<IllegalStateException> {
            moderationService.approveProduct(PRODUCT_ID, MODERATOR_ID)
        }
        
        assertEquals("Может быть одобрен только товар со статусом PENDING", ex.message)
        verify(productRepository, never()).save(any())
    }

    // ==========================================
    // 2. Tests for rejectProduct
    // ==========================================

    @Test
    fun `rejectProduct should succeed when product is PENDING`() {
        val pendingProduct = createProduct(ProductStatus.PENDING)

        whenever(productRepository.findById(eq(PRODUCT_ID)))
            .thenReturn(Optional.of(pendingProduct))

        val productCaptor = argumentCaptor<Product>()
        whenever(productRepository.save(productCaptor.capture())).thenAnswer { 
            it.arguments[0] as Product 
        }

        val result = moderationService.rejectProduct(PRODUCT_ID, MODERATOR_ID, REJECT_REASON)

        assertEquals(ProductStatus.REJECTED.name, result.status)
        assertEquals(REJECT_REASON, result.rejectionReason)

        val capturedProduct = productCaptor.firstValue
        assertEquals(ProductStatus.REJECTED, capturedProduct.status)
        assertEquals(REJECT_REASON, capturedProduct.rejectionReason)
    }

    @Test
    fun `rejectProduct should throw exception when status is not PENDING`() {
        val rejectedProduct = createProduct(ProductStatus.REJECTED)
        
        whenever(productRepository.findById(eq(PRODUCT_ID)))
            .thenReturn(Optional.of(rejectedProduct))

        val ex = assertThrows<IllegalStateException> {
            moderationService.rejectProduct(PRODUCT_ID, MODERATOR_ID, REJECT_REASON)
        }
        
        assertEquals("Может быть отклонен только товар со статусом PENDING", ex.message)
        verify(productRepository, never()).save(any())
    }

    @Test
    fun `rejectProduct should throw exception when product not found`() {
        whenever(productRepository.findById(eq(PRODUCT_ID)))
            .thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            moderationService.rejectProduct(PRODUCT_ID, MODERATOR_ID, REJECT_REASON)
        }
    }

    // ==========================================
    // 3. Tests for getPendingProducts (Pagination)
    // ==========================================

    @Test
    fun `getPendingProducts should return populated page`() {
        val page = 1
        val pageSize = 10
        val pendingProduct = createProduct(ProductStatus.PENDING)
        
        // Create a page with one product
        val productPage = PageImpl(listOf(pendingProduct))
        val pageable = PageRequest.of(0, pageSize) // Service does page - 1

        whenever(productRepository.findAllByStatus(eq(ProductStatus.PENDING), eq(pageable)))
            .thenReturn(productPage)
            
        // Mock comment stats
        whenever(commentRepository.getAverageRatingByProductId(eq(PRODUCT_ID))).thenReturn(4.5)
        whenever(commentRepository.getCommentCountByProductId(eq(PRODUCT_ID))).thenReturn(12L)

        val result = moderationService.getPendingProducts(page, pageSize)

        assertEquals(1, result.data.size)
        assertEquals(4.5, result.data[0].averageRating)
        assertEquals(12L, result.data[0].commentsCount)
        assertEquals(PRODUCT_ID, result.data[0].id)
        
        // Verify interactions
        verify(productRepository).findAllByStatus(any(), any())
        verify(commentRepository).getAverageRatingByProductId(eq(PRODUCT_ID))
    }

    @Test
    fun `getPendingProducts should handle empty result`() {
        val page = 1
        val pageSize = 10
        val pageable = PageRequest.of(0, pageSize)
        
        whenever(productRepository.findAllByStatus(eq(ProductStatus.PENDING), eq(pageable)))
            .thenReturn(PageImpl(emptyList()))

        val result = moderationService.getPendingProducts(page, pageSize)

        assertTrue(result.data.isEmpty())
        verify(commentRepository, never()).getAverageRatingByProductId(any())
    }

    // ==========================================
    // 4. Tests for getPendingProductById
    // ==========================================

    @Test
    fun `getPendingProductById should return details when status is PENDING`() {
        val pendingProduct = createProduct(ProductStatus.PENDING)
        
        whenever(productRepository.findById(eq(PRODUCT_ID)))
            .thenReturn(Optional.of(pendingProduct))
        
        whenever(commentRepository.getAverageRatingByProductId(eq(PRODUCT_ID))).thenReturn(5.0)
        whenever(commentRepository.getCommentCountByProductId(eq(PRODUCT_ID))).thenReturn(2L)

        val result = moderationService.getPendingProductById(PRODUCT_ID)

        assertEquals(PRODUCT_ID, result.id)
        assertEquals(ProductStatus.PENDING.name, result.status)
        assertEquals(5.0, result.averageRating)
    }

    @Test
    fun `getPendingProductById should throw exception when status is NOT PENDING`() {
        // This covers the specific line: throw ResourceNotFoundException("Товар не на модерации")
        val approvedProduct = createProduct(ProductStatus.APPROVED)
        
        whenever(productRepository.findById(eq(PRODUCT_ID)))
            .thenReturn(Optional.of(approvedProduct))

        val ex = assertThrows<ResourceNotFoundException> {
            moderationService.getPendingProductById(PRODUCT_ID)
        }
        
        assertEquals("Товар не на модерации", ex.message)
    }

    @Test
    fun `getPendingProductById should throw exception when not found`() {
        whenever(productRepository.findById(eq(PRODUCT_ID)))
            .thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            moderationService.getPendingProductById(PRODUCT_ID)
        }
    }

    // Helper method to create dummy entities
    private fun createProduct(status: ProductStatus): Product {
        return Product(
            id = PRODUCT_ID,
            name = "Test Item",
            description = "Desc",
            price = BigDecimal("50.00"),
            imageUrl = "http://img.com/1",
            shopId = SHOP_ID,
            sellerId = SELLER_ID,
            status = status,
            rejectionReason = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}