package ru.itmo.market.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import ru.itmo.market.exception.ForbiddenException
import ru.itmo.market.exception.ResourceNotFoundException
import ru.itmo.market.model.entity.Product
import ru.itmo.market.model.entity.Shop
import ru.itmo.market.model.entity.User
import ru.itmo.market.model.enums.ProductStatus
import ru.itmo.market.model.enums.UserRole
import ru.itmo.market.repository.CommentRepository
import ru.itmo.market.repository.ProductRepository
import ru.itmo.market.repository.ShopRepository
import ru.itmo.market.repository.UserRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class ShopServiceUnitTest {

    @Mock
    private lateinit var shopRepository: ShopRepository
    @Mock
    private lateinit var productRepository: ProductRepository
    @Mock
    private lateinit var userRepository: UserRepository
    @Mock
    private lateinit var commentRepository: CommentRepository

    private lateinit var shopService: ShopService

    // Constants
    private val SELLER_ID = 1L
    private val SHOP_ID = 100L
    private val DUMMY_USER = User(
        id = SELLER_ID, username = "u", email = "e", password = "p",
        firstName = "John", lastName = "Doe", roles = emptySet()
    )

    @BeforeEach
    fun setUp() {
        shopService = ShopService(
            shopRepository = shopRepository,
            productRepository = productRepository,
            userRepository = userRepository,
            commentRepository = commentRepository
        )
    }

    /**
     * HELPER: Sets up mocks required by the private 'toResponse()' method.
     * This is needed for Create, Update, and GetById tests.
     */
    private fun setupMocksForToResponse() {
        // 1. Mock finding the seller (fixes ResourceNotFoundException)
        whenever(userRepository.findById(eq(SELLER_ID))).thenReturn(Optional.of(DUMMY_USER))

        // 2. Mock the product count (fixes NullPointerException in extension function)
        // The extension function calls findAllByShopId with PageRequest.of(0, 1)
        whenever(productRepository.findAllByShopId(eq(SHOP_ID), any()))
            .thenReturn(Page.empty())
    }

    @Test
    fun `should get shop by id successfully`() {
        val shop = Shop(id = SHOP_ID, name = "Test Shop", sellerId = SELLER_ID)
        whenever(shopRepository.findById(eq(SHOP_ID))).thenReturn(Optional.of(shop))

        // Prepare dependencies for toResponse()
        setupMocksForToResponse()

        val result = shopService.getShopById(SHOP_ID)

        assertNotNull(result)
        assertEquals(SHOP_ID, result.id)
        assertEquals("John Doe", result.sellerName)
        verify(shopRepository, times(1)).findById(SHOP_ID)
    }

 @Test
    fun `should create shop successfully`() {
        val sellerId = 1L
        val shopId = 1L
        val name = "New Shop"
        val description = "New Description"
        val avatarUrl = "https://example.com/avatar.jpg"

        val user = User(
            id = sellerId,
            username = "seller",
            email = "seller@example.com",
            password = "hashed",
            firstName = "John",
            lastName = "Doe",
            roles = setOf(UserRole.SELLER)
        )

        val productList = listOf<Product>()
        val pageable = PageRequest.of(0, 1)
        val productPage: Page<Product> = PageImpl(productList, pageable, 0)

        whenever(userRepository.findById(eq(sellerId))).thenReturn(Optional.of(user))
        whenever(productRepository.findAllByShopId(eq(shopId), any())).thenReturn(productPage)

        doReturn(Shop(id = shopId, name = name, description = description, avatarUrl = avatarUrl, sellerId = sellerId))
            .whenever(shopRepository).save(any())

        val result = shopService.createShop(sellerId, name, description, avatarUrl)

        assertNotNull(result)
        assertEquals(name, result.name)
        assertEquals(description, result.description)
        assertEquals(avatarUrl, result.avatarUrl)

        verify(shopRepository, times(1)).save(any())
    }

    @Test
    fun `should throw exception when shop creation with empty name`() {
        // No mocks needed because it fails fast
        assertThrows<IllegalArgumentException> {
            shopService.createShop(SELLER_ID, "", null, null)
        }
        verify(shopRepository, never()).save(any())
    }

    // ========================================================
    //  NEW TESTS FOR: updateShop (Covers Red Lines)
    // ========================================================
@Test
    fun `updateShop should update fields and return response when user is owner`() {
        // Arrange
        val oldShop = Shop(
            id = SHOP_ID, sellerId = SELLER_ID, 
            name = "Old Name", description = "Old Desc", avatarUrl = "Old Url"
        )
        
        whenever(shopRepository.findById(SHOP_ID)).thenReturn(Optional.of(oldShop))
        
        // ✅ FIX: Use doAnswer().whenever(...) to avoid NPE on non-nullable return types
        doAnswer { invocation -> 
            invocation.arguments[0] as Shop 
        }.whenever(shopRepository).save(any())

        // Prepare dependencies for toResponse()
        setupMocksForToResponse()

        // Act
        val result = shopService.updateShop(SHOP_ID, SELLER_ID, "New Name", "New Desc", null)

        // Assert
        assertEquals("New Name", result.name)
        assertEquals("New Desc", result.description)
        assertEquals("Old Url", result.avatarUrl) 
        assertEquals("John Doe", result.sellerName)
        
        verify(shopRepository).save(any())
    }

    @Test
    fun `updateShop should throw ForbiddenException if user is not the owner`() {
        val shop = Shop(id = SHOP_ID, sellerId = SELLER_ID, name = "Shop")
        whenever(shopRepository.findById(SHOP_ID)).thenReturn(Optional.of(shop))

        val intruderId = 999L

        assertThrows<ForbiddenException> {
            shopService.updateShop(SHOP_ID, intruderId, "New Name", null, null)
        }

        verify(shopRepository, never()).save(any())
    }

    @Test
    fun `updateShop should throw ResourceNotFoundException if shop missing`() {
        whenever(shopRepository.findById(SHOP_ID)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            shopService.updateShop(SHOP_ID, SELLER_ID, "Name", null, null)
        }
    }

    // ========================================================
    //  NEW TESTS FOR: getShopProducts (Covers Red Lines)
    // ========================================================

    @Test
    fun `getShopProducts should return paginated and mapped product list`() {
        // 1. Check shop existence
        val shop = Shop(id = SHOP_ID, sellerId = SELLER_ID, name = "Shop")
        whenever(shopRepository.findById(SHOP_ID)).thenReturn(Optional.of(shop))

        // 2. Mock Products Page
        val product = Product(
            id = 50L, name = "Test Product", price = BigDecimal("100.00"),
            shopId = SHOP_ID, sellerId = SELLER_ID, status = ProductStatus.APPROVED,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
        )
        val pageRequest = PageRequest.of(0, 10) // Service receives page 1 -> creates Request 0
        val productPage = PageImpl(listOf(product), pageRequest, 1)

        whenever(productRepository.findAllByShopId(eq(SHOP_ID), any())).thenReturn(productPage)

        // 3. Mock Comment Stats (The mapping logic)
        whenever(commentRepository.getAverageRatingByProductId(50L)).thenReturn(4.5)
        whenever(commentRepository.getCommentCountByProductId(50L)).thenReturn(10L)

        // Act
        val result = shopService.getShopProducts(SHOP_ID, 1, 10)

        // Assert
        assertEquals(1, result.data.size)
        assertEquals(1, result.totalPages)
        
        val item = result.data[0]
        assertEquals(50L, item.id)
        assertEquals(4.5, item.averageRating) // Verified mapping
        assertEquals(10L, item.commentsCount) // Verified mapping
    }

    @Test
    fun `getShopProducts should throw exception if shop does not exist`() {
        whenever(shopRepository.findById(SHOP_ID)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            shopService.getShopProducts(SHOP_ID, 1, 10)
        }

        verify(productRepository, never()).findAllByShopId(any(), any())
    }
}