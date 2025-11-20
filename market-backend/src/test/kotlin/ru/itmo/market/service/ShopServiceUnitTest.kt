package ru.itmo.market.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import ru.itmo.market.exception.ConflictException
import ru.itmo.market.model.dto.response.ShopResponse
import ru.itmo.market.model.entity.Product
import ru.itmo.market.model.entity.Shop
import ru.itmo.market.model.entity.User
import ru.itmo.market.model.enums.UserRole
import ru.itmo.market.repository.ShopRepository
import ru.itmo.market.repository.ProductRepository
import ru.itmo.market.repository.UserRepository
import ru.itmo.market.repository.CommentRepository
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

    @BeforeEach
    fun setUp() {
        shopService = ShopService(
            shopRepository = shopRepository,
            productRepository = productRepository,
            userRepository = userRepository,
            commentRepository = commentRepository
        )
    }

    @Test
    fun `should get shop by id successfully`() {
        val shopId = 100L
        val sellerId = 1L

        val shop = Shop(
            id = shopId,
            name = "Test Shop",
            description = "Test Description",
            sellerId = sellerId,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val seller = User(
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

        whenever(shopRepository.findById(eq(shopId))).thenReturn(Optional.of(shop))
        whenever(productRepository.findAllByShopId(eq(shopId), any())).thenReturn(productPage)
        whenever(userRepository.findById(eq(sellerId))).thenReturn(Optional.of(seller))

        val result = shopService.getShopById(shopId)

        assertNotNull(result)
        assertEquals(shopId, result.id)
        assertEquals("Test Shop", result.name)

        verify(shopRepository, times(1)).findById(shopId)
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
    @Disabled
    fun `should throw exception when shop creation with empty name`() {
        val sellerId = 1L

        val user = User(
            id = sellerId,
            username = "seller",
            email = "seller@example.com",
            password = "hashed",
            firstName = "John",
            lastName = "Doe",
            roles = setOf(UserRole.SELLER)
        )

        whenever(userRepository.findById(eq(sellerId))).thenReturn(Optional.of(user))

        assertThrows<Exception> {
            shopService.createShop(sellerId, "", null, null)
        }

        verify(shopRepository, never()).save(any())
    }
}
