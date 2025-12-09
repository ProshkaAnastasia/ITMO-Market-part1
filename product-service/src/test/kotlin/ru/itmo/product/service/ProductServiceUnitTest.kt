package ru.itmo.product.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import ru.itmo.product.exception.ForbiddenException
import ru.itmo.product.model.dto.request.UpdateProductRequest
import ru.itmo.product.model.dto.response.ShopResponse
import ru.itmo.product.model.dto.response.UserResponse
import ru.itmo.product.model.entity.Product
import ru.itmo.product.model.enums.ProductStatus
import ru.itmo.product.repository.ProductRepository
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
    @Mock
    private lateinit var userService: UserService

    private lateinit var productService: ProductService

    private val SELLER_ID = 1L
    private val SHOP_ID = 1L
    private val MODERATOR_ID = 99L
    private val PRODUCT_ID = 100L
    
    private val USER_ID = 3L 

    @BeforeEach
    fun setUp() {
        productService = ProductService(
            productRepository,
            shopService,
            commentService,
            userService
        )
    }

    private fun mockUser(userId: Long, roles: Set<String>) {
        val user = UserResponse(
            id = userId,
            username = "user$userId",
            email = "email",
            firstName = "Fn",
            lastName = "Ln",
            roles = roles,
            createdAt = LocalDateTime.now()
        )
        whenever(userService.getUserById(userId)).thenReturn(user)
    }

    private fun mockCommentStats(productId: Long) {
        whenever(commentService.getAverageRatingByProductId(productId)).thenReturn(5.0)
        whenever(commentService.getCommentCountByProductId(productId)).thenReturn(10L)
    }

    
    
    

    @Test
    fun `createProduct should succeed for shop owner`() {
        
        val shopDto = ShopResponse(
            id = SHOP_ID, name = "Shop", description = null, avatarUrl = null,
            sellerId = SELLER_ID, sellerName = "Seller", productsCount = 0,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
        )
        whenever(shopService.getShopById(SHOP_ID)).thenReturn(shopDto)

        doAnswer { 
            (it.arguments[0] as Product).copy(id = PRODUCT_ID) 
        }.whenever(productRepository).save(any())
        mockCommentStats(PRODUCT_ID)

        
        val result = productService.createProduct("Prod", "Desc", BigDecimal("10"), null, SHOP_ID, SELLER_ID)

        
        assertEquals(ProductStatus.PENDING.name, result.status)
        verify(productRepository).save(any())
    }

    @Test
    fun `updateProduct should succeed for Moderator`() {
        val product = Product(id = PRODUCT_ID, name = "Old", price = BigDecimal("1"), shopId = SHOP_ID, sellerId = SELLER_ID, status = ProductStatus.APPROVED, createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
        
        whenever(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product))
        mockUser(MODERATOR_ID, setOf("MODERATOR")) 
        
        doAnswer { 
            it.arguments[0] as Product 
        }.whenever(productRepository).save(any())
        mockCommentStats(PRODUCT_ID)

        val request = UpdateProductRequest(name = "New Name", description = null, price = null, imageUrl = null)
        
        val result = productService.updateProduct(PRODUCT_ID, MODERATOR_ID, request)

        assertEquals("New Name", result.name)
    }

    
    
    

    @Test
    fun `approveProduct should succeed for Moderator`() {
        
        mockUser(MODERATOR_ID, setOf("MODERATOR"))
        
        val pendingProduct = Product(
            id = PRODUCT_ID, name = "Item", price = BigDecimal("10"), shopId = SHOP_ID, sellerId = SELLER_ID,
            status = ProductStatus.PENDING, createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
        )
        whenever(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(pendingProduct))
        
        doAnswer { 
            it.arguments[0] as Product 
        }.whenever(productRepository).save(any())

        mockCommentStats(PRODUCT_ID)

        
        val result = productService.approveProduct(PRODUCT_ID, MODERATOR_ID)

        
        assertEquals(ProductStatus.APPROVED.name, result.status)
        verify(productRepository).save(check {
            assertEquals(ProductStatus.APPROVED, it.status)
        })
    }

    @Test
    fun `approveProduct should fail for simple User`() {
        
        mockUser(USER_ID, setOf("USER")) 

        assertThrows<ForbiddenException> {
            
            productService.approveProduct(PRODUCT_ID, USER_ID) 
        }
        verify(productRepository, never()).save(any())
    }

    @Test
    fun `approveProduct should fail if status is not PENDING`() {
        mockUser(MODERATOR_ID, setOf("MODERATOR"))
        
        val approvedProduct = Product(id = PRODUCT_ID, name = "Item", price = BigDecimal("10"), shopId = SHOP_ID, sellerId = SELLER_ID, status = ProductStatus.APPROVED, createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
        whenever(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(approvedProduct))

        val ex = assertThrows<IllegalStateException> {
            productService.approveProduct(PRODUCT_ID, MODERATOR_ID)
        }
        assertEquals("Может быть одобрен только товар со статусом PENDING", ex.message)
    }

    @Test
    fun `rejectProduct should succeed and set reason`() {
        mockUser(MODERATOR_ID, setOf("ADMIN")) 
        
        val pendingProduct = Product(id = PRODUCT_ID, name = "Item", price = BigDecimal("10"), shopId = SHOP_ID, sellerId = SELLER_ID, status = ProductStatus.PENDING, createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
        whenever(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(pendingProduct))
        doAnswer { 
            it.arguments[0] as Product 
        }.whenever(productRepository).save(any())

        mockCommentStats(PRODUCT_ID)

        val result = productService.rejectProduct(PRODUCT_ID, MODERATOR_ID, "Bad content")

        assertEquals(ProductStatus.REJECTED.name, result.status)
        assertEquals("Bad content", result.rejectionReason)
    }
}