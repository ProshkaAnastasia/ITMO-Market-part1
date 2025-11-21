package ru.itmo.market.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import ru.itmo.market.exception.ForbiddenException
import ru.itmo.market.exception.ResourceNotFoundException
import ru.itmo.market.model.dto.response.ShopResponse
import ru.itmo.market.model.entity.Product
import ru.itmo.market.model.enums.ProductStatus
import ru.itmo.market.repository.ProductRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class ProductServiceUnitTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var shopService: ShopService

    @Mock
    private lateinit var commentService: CommentService

    private lateinit var productService: ProductService

    @BeforeEach
    fun setUp() {
        productService = ProductService(
            productRepository,
            shopService,
            commentService
        )
    }

    @Test
    fun `should get product by id successfully`() {
        val productId = 100L
        val product = Product(
            id = productId, name = "Test Product", description = "Test Description",
            price = BigDecimal("99.99"), shopId = 1L, sellerId = 1L,
            status = ProductStatus.APPROVED, createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
        )

        whenever(productRepository.findById(eq(productId))).thenReturn(Optional.of(product))
        
        whenever(commentService.getAverageRatingByProductId(eq(productId))).thenReturn(4.5)
        whenever(commentService.getCommentCountByProductId(eq(productId))).thenReturn(10L)

        val result = productService.getProductById(productId)

        assertNotNull(result)
        assertEquals(productId, result.id)
    }

    @Test
    fun `should create product successfully`() {
        val shopId = 1L
        val sellerId = 1L
        val price = BigDecimal("149.99")

        // Mock ShopService returning a DTO (ShopResponse), NOT an Entity (Shop)
        val mockShopResponse = ShopResponse(
            id = shopId, name = "Test Shop", description = "Desc", avatarUrl = "Url",
            sellerId = sellerId, sellerName = "Seller", productsCount = 0,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
        )
        // ✅ FIXED: Use shopService.getShopById instead of repository.findById
        whenever(shopService.getShopById(eq(shopId))).thenReturn(mockShopResponse)

        val productCaptor = argumentCaptor<Product>()
        whenever(productRepository.save(productCaptor.capture())).thenAnswer {
            (it.arguments[0] as Product).copy(id = 1L)
        }

        val result = productService.createProduct("New Product", "Desc", price, "url", shopId, sellerId)

        assertNotNull(result)
        assertEquals(ProductStatus.PENDING.name, result.status)
        verify(shopService).getShopById(shopId)
        verify(productRepository).save(any())
    }

    @Test
    fun `should throw ForbiddenException when creating product for shop not owned`() {
        val shopId = 1L
        val sellerId = 1L
        val intruderId = 999L

        val mockShopResponse = ShopResponse(
            id = shopId, name = "Test Shop", description = "Desc", avatarUrl = "Url",
            sellerId = sellerId, sellerName = "Seller", productsCount = 0,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
        )
        whenever(shopService.getShopById(eq(shopId))).thenReturn(mockShopResponse)

        assertThrows<ForbiddenException> {
            productService.createProduct("Name", "Desc", BigDecimal("10"), null, shopId, intruderId)
        }
    }

    @Test
    fun `should throw ResourceNotFoundException when updating non-existent product`() {
        val productId = 999L
        whenever(productRepository.findById(productId)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            productService.getProductById(productId)
        }
    }
}