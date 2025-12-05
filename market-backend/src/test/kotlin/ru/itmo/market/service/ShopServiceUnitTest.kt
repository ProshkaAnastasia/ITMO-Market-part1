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
import ru.itmo.market.model.dto.response.PaginatedResponse
import ru.itmo.market.model.dto.response.ProductResponse
import ru.itmo.market.model.dto.response.UserResponse
import ru.itmo.market.model.entity.Product
import ru.itmo.market.model.entity.Shop
import ru.itmo.market.model.entity.User
import ru.itmo.market.model.enums.ProductStatus
import ru.itmo.market.model.enums.UserRole
// ❌ УДАЛЕНЫ НЕНУЖНЫЕ ИМПОРТЫ РЕПОЗИТОРИЕВ
// import ru.itmo.market.repository.CommentRepository 
// import ru.itmo.market.repository.ProductRepository
// import ru.itmo.market.repository.UserRepository
import ru.itmo.market.repository.ShopRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class ShopServiceUnitTest {

    @Mock
    private lateinit var shopRepository: ShopRepository
    
    // ✅ ИСПОЛЬЗУЕМ СЕРВИСЫ
    @Mock
    private lateinit var productService: ProductService 
    @Mock
    private lateinit var userService: UserService

    private lateinit var shopService: ShopService

    // Constants
    private val SELLER_ID = 1L
    private val SHOP_ID = 100L
    
    // ✅ Добавлен DTO для мокирования UserService
    private val DUMMY_USER_RESPONSE = UserResponse(
        id = SELLER_ID, username = "u", email = "e", firstName = "John", 
        lastName = "Doe", roles = emptySet(), createdAt = LocalDateTime.now()
    )


@BeforeEach
    fun setUp() {
        shopService = ShopService(
            shopRepository,
            productService, 
            userService    
        )
    }

    /**
     * HELPER: Sets up mocks required by the private 'convertToResponse()' method.
     * This is needed for Create, Update, and GetById tests.
     */
    private fun setupMocksForConvertToResponse() {
        // 1. Mock finding the seller (fixes ResourceNotFoundException)
        // ✅ FIX: Используем userService.getUserById()
        whenever(userService.getUserById(eq(SELLER_ID))).thenReturn(DUMMY_USER_RESPONSE)

        // 2. Mock the product count (fixes NullPointerException in toResponse())
        // ✅ FIX: Используем productService.countProductsByShopId()
        whenever(productService.countProductsByShopId(eq(SHOP_ID))).thenReturn(0L) 
    }
    
    // Helper для создания ProductResponse (для мокирования getShopProducts)
    private fun createProductResponse(productId: Long, shopId: Long, rating: Double = 0.0, count: Long = 0L): ProductResponse {
        return ProductResponse(
            id = productId, name = "Test Product", description = "Desc",
            price = BigDecimal("100.00"), imageUrl = null, shopId = shopId, 
            sellerId = SELLER_ID, status = ProductStatus.APPROVED.name, 
            rejectionReason = null, averageRating = rating, commentsCount = count,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
        )
    }

    @Test
    fun `should get shop by id successfully`() {
        val shop = Shop(id = SHOP_ID, name = "Test Shop", sellerId = SELLER_ID)
        whenever(shopRepository.findById(eq(SHOP_ID))).thenReturn(Optional.of(shop))

        // Prepare dependencies for convertToResponse()
        // ✅ FIX: Называем хелпер корректно
        setupMocksForConvertToResponse() 

        val result = shopService.getShopById(SHOP_ID)

        assertNotNull(result)
        assertEquals(SHOP_ID, result.id)
        assertEquals("John Doe", result.sellerName)
        verify(shopRepository, times(1)).findById(SHOP_ID)
        // ✅ FIX: Проверяем, что сервисы были вызваны
        verify(userService, times(1)).getUserById(SELLER_ID)
        verify(productService, times(1)).countProductsByShopId(SHOP_ID)
    }

 @Test
    fun `should create shop successfully`() {
        val sellerId = SELLER_ID
        val shopId = SHOP_ID 
        val name = "New Shop"
        val description = "New Description"
        val avatarUrl = "https://example.com/avatar.jpg"

        // ✅ FIX: Настраиваем моки ОДИН раз
        whenever(userService.getUserById(eq(sellerId))).thenReturn(DUMMY_USER_RESPONSE)
        whenever(productService.countProductsByShopId(eq(shopId))).thenReturn(0L)

        // Мокаем save. Важно: сервис сначала проверяет existsBySellerId (вернет false по умолчанию),
        // затем сохраняет, затем вызывает toResponse (который дергает userService и productService).
        doReturn(Shop(id = shopId, name = name, description = description, avatarUrl = avatarUrl, sellerId = sellerId))
            .whenever(shopRepository).save(any())
        
        // ❌ УДАЛЕНО дублирование whenever(userService...), которое ломало тест

        val result = shopService.createShop(sellerId, name, description, avatarUrl)

        assertNotNull(result)
        assertEquals(name, result.name)
        assertEquals(description, result.description)

        verify(shopRepository, times(1)).save(any())
        // Проверяем, что toResponse отработал
        verify(productService, times(1)).countProductsByShopId(shopId)
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
    //  Tests for updateShop
    // ========================================================
    
    @Test
    fun `updateShop should update fields and return response when user is owner`() {
        // Arrange
        val oldShop = Shop(
            id = SHOP_ID, sellerId = SELLER_ID, 
            name = "Old Name", description = "Old Desc", avatarUrl = "Old Url"
        )
        
        whenever(shopRepository.findById(SHOP_ID)).thenReturn(Optional.of(oldShop))
        
        doAnswer { invocation -> 
            invocation.arguments[0] as Shop 
        }.whenever(shopRepository).save(any())

        // Prepare dependencies for convertToResponse()
        setupMocksForConvertToResponse() // ✅ FIX: Вызов корректного хелпера

        // Act
        val result = shopService.updateShop(SHOP_ID, SELLER_ID, "New Name", "New Desc", null)

        // Assert
        assertEquals("New Name", result.name)
        assertEquals("New Desc", result.description)
        assertEquals("Old Url", result.avatarUrl) 
        assertEquals("John Doe", result.sellerName)
        
        verify(shopRepository).save(any())
        verify(userService, times(1)).getUserById(SELLER_ID) // ✅ Проверка
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
    //  Tests for getShopProducts
    // ========================================================

@Test
    fun `getShopProducts should return paginated and mapped product list`() {
        // 1. Check shop existence
        // ✅ FIX: Сервис теперь использует existsById, а не findById.
        // Нам не нужно создавать объект Shop, достаточно вернуть true.
        whenever(shopRepository.existsById(SHOP_ID)).thenReturn(true)

        // 2. Prepare Product DTO
        val expectedRating = 4.5
        val expectedCount = 10L
        val productDto = createProductResponse(50L, SHOP_ID, expectedRating, expectedCount)
        
        // 3. Mock the result of delegation to ProductService
        val paginatedResponse = PaginatedResponse(
            data = listOf(productDto),
            page = 1,
            pageSize = 10,
            totalElements = 1,
            totalPages = 1
        )

        whenever(productService.getProductsByShopId(eq(SHOP_ID), eq(1), eq(10)))
            .thenReturn(paginatedResponse)

        // Act
        val result = shopService.getShopProducts(SHOP_ID, 1, 10)

        // Assert
        assertEquals(1, result.data.size)
        assertEquals(1, result.totalPages)
        
        val item = result.data[0]
        assertEquals(50L, item.id)
        
        // ✅ Проверяем вызов existsById
        verify(shopRepository, times(1)).existsById(SHOP_ID)
        verify(productService, times(1)).getProductsByShopId(SHOP_ID, 1, 10)
    }

    @Test
    fun `getShopProducts should throw exception if shop does not exist`() {
        // ✅ FIX: Мокаем existsById, чтобы он вернул false.
        // Старый мок findById вызывал ошибку UnnecessaryStubbingException, так как сервис его не вызывал.
        whenever(shopRepository.existsById(SHOP_ID)).thenReturn(false)

        assertThrows<ResourceNotFoundException> {
            shopService.getShopProducts(SHOP_ID, 1, 10)
        }

        verify(productService, never()).getProductsByShopId(any(), any(), any()) 
    }
}