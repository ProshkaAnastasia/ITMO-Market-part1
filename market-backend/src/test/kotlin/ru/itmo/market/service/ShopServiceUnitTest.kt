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
import ru.itmo.market.model.dto.response.UserResponse
import ru.itmo.market.model.entity.Shop
import ru.itmo.market.repository.ShopRepository
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class ShopServiceUnitTest {

    @Mock
    private lateinit var shopRepository: ShopRepository
    @Mock
    private lateinit var productService: ProductService
    @Mock
    private lateinit var userService: UserService

    private lateinit var shopService: ShopService

    private val SELLER_ID = 1L
    private val SHOP_ID = 100L

    @BeforeEach
    fun setUp() {
        shopService = ShopService(
            shopRepository,
            productService,
            userService
        )
    }

    // Helper to mock the calls made by convertToResponse()
    private fun setupMocksForToResponse() {
        val userResponse = UserResponse(
            id = SELLER_ID,
            username = "seller",
            email = "email@test.com",
            firstName = "John",
            lastName = "Doe",
            roles = emptySet(),
            createdAt = LocalDateTime.now()
        )
        whenever(userService.getUserById(eq(SELLER_ID))).thenReturn(userResponse)
        whenever(productService.countProductsByShopId(eq(SHOP_ID))).thenReturn(5L)
    }

    @Test
    fun `should get shop by id successfully`() {
        val shop = Shop(id = SHOP_ID, name = "Test Shop", sellerId = SELLER_ID)
        whenever(shopRepository.findById(eq(SHOP_ID))).thenReturn(Optional.of(shop))

        setupMocksForToResponse()

        val result = shopService.getShopById(SHOP_ID)

        assertNotNull(result)
        assertEquals(SHOP_ID, result.id)
        assertEquals("John Doe", result.sellerName)
    }

    @Test
    fun `should create shop successfully`() {
        whenever(shopRepository.existsBySellerId(SELLER_ID)).thenReturn(false)

        doAnswer { invocation ->
            val s = invocation.arguments[0] as Shop
            s.copy(id = SHOP_ID)
        }.whenever(shopRepository).save(any())

        setupMocksForToResponse()

        val result = shopService.createShop(SELLER_ID, "New Shop", "Desc", "Url")

        assertNotNull(result)
        assertEquals(SHOP_ID, result.id)
    }

    // ==========================================
    // 🆕 Tests for updateShop
    // ==========================================

    // @Test
    // fun `updateShop should update fields and return response when user is owner`() {
    //     // 1. Arrange
    //     val oldShop = Shop(
    //         id = SHOP_ID,
    //         sellerId = SELLER_ID,
    //         name = "Old Name",
    //         description = "Old Desc",
    //         avatarUrl = "Old Url",
    //         createdAt = LocalDateTime.now(),
    //         updatedAt = LocalDateTime.now()
    //     )

    //     whenever(shopRepository.findById(SHOP_ID)).thenReturn(Optional.of(oldShop))

    //     // Mock save to return the updated object passed to it
    //     whenever(shopRepository.save(any())).thenAnswer { it.arguments[0] as Shop }

    //     // Prepare dependencies for the response conversion
    //     setupMocksForToResponse()

    //     // 2. Act
    //     // We update Name and Description, but pass NULL for avatarUrl to test that it keeps the old one
    //     val result = shopService.updateShop(
    //         shopId = SHOP_ID,
    //         userId = SELLER_ID,
    //         name = "New Name",
    //         description = "New Desc",
    //         avatarUrl = null
    //     )

    //     // 3. Assert
    //     assertEquals("New Name", result.name)       // Updated
    //     assertEquals("New Desc", result.description)// Updated
    //     assertEquals("Old Url", result.avatarUrl)   // Kept original (Elvis operator check)
    //     assertEquals("John Doe", result.sellerName) // Mapped correctly

    //     // Verify that save was called
    //     verify(shopRepository).save(check {
    //         assertEquals("New Name", it.name)
    //         assertEquals("Old Url", it.avatarUrl)
    //     })
    // }

    @Test
    fun `updateShop should throw ForbiddenException if user is not the owner`() {
        val shop = Shop(id = SHOP_ID, sellerId = SELLER_ID, name = "Shop")
        whenever(shopRepository.findById(SHOP_ID)).thenReturn(Optional.of(shop))

        val intruderId = 999L

        val ex = assertThrows<ForbiddenException> {
            shopService.updateShop(SHOP_ID, intruderId, "New Name", null, null)
        }

        assertEquals("У вас нет прав для обновления этого магазина", ex.message)
        verify(shopRepository, never()).save(any())
    }

    @Test
    fun `updateShop should throw ResourceNotFoundException if shop missing`() {
        whenever(shopRepository.findById(SHOP_ID)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            shopService.updateShop(SHOP_ID, SELLER_ID, "Name", null, null)
        }

        verify(shopRepository, never()).save(any())
    }
}