package ru.itmo.user.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import ru.itmo.user.exception.ConflictException
import ru.itmo.user.exception.ForbiddenException
import ru.itmo.user.exception.ResourceNotFoundException
import ru.itmo.user.exception.ServiceUnavailableException
import ru.itmo.user.model.dto.response.PaginatedResponse
import ru.itmo.user.model.dto.response.ProductResponse
import ru.itmo.user.model.dto.response.UserResponse
import ru.itmo.user.model.entity.Shop
import ru.itmo.user.repository.ShopRepository
import ru.itmo.user.service.client.ProductServiceClient
import java.math.BigDecimal
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class ShopServiceUnitTest {

    @Mock
    private lateinit var shopRepository: ShopRepository

    @Mock
    private lateinit var productServiceClient: ProductServiceClient

    @Mock
    private lateinit var userService: UserService

    private lateinit var shopService: ShopService

    private val sellerId = 1L
    private val shopId = 100L
    private val testShop = Shop(
        id = shopId,
        name = "Test Shop",
        description = "Test Description",
        avatarUrl = "https://example.com/avatar.jpg",
        sellerId = sellerId,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private val testUserResponse = UserResponse(
        id = sellerId,
        username = "seller",
        email = "seller@example.com",
        firstName = "John",
        lastName = "Doe",
        roles = setOf("SELLER"),
        createdAt = LocalDateTime.now()
    )

    @BeforeEach
    fun setUp() {
        shopService = ShopService(shopRepository, productServiceClient, userService)
    }

    @Test
    fun `getAllShops should return paginated list of shops`() {
        val shop1 = testShop.copy(id = 1L)
        val shop2 = testShop.copy(id = 2L, name = "Another Shop")

        whenever(shopRepository.findAll()).thenReturn(Flux.just(shop1, shop2))
        whenever(shopRepository.count()).thenReturn(Mono.just(2L))
        whenever(userService.getUserById(sellerId)).thenReturn(Mono.just(testUserResponse))
        whenever(productServiceClient.countProductsByShopId(any())).thenReturn(5L)

        StepVerifier.create(shopService.getAllShops(1, 10))
            .expectNextMatches { response ->
                response.data.size == 2 &&
                response.page == 1 &&
                response.pageSize == 10 &&
                response.totalElements == 2L &&
                response.totalPages == 1 &&
                response.data[0].id == 1L &&
                response.data[1].id == 2L
            }
            .verifyComplete()

        verify(shopRepository).findAll()
        verify(shopRepository).count()
        verify(userService, times(2)).getUserById(sellerId)
        verify(productServiceClient, times(2)).countProductsByShopId(any())
    }

    @Test
    fun `getAllShops should handle pagination correctly`() {
        val shops = (1L..5L).map { testShop.copy(id = it) }

        whenever(shopRepository.findAll()).thenReturn(Flux.fromIterable(shops))
        whenever(shopRepository.count()).thenReturn(Mono.just(5L))
        whenever(userService.getUserById(sellerId)).thenReturn(Mono.just(testUserResponse))
        whenever(productServiceClient.countProductsByShopId(any())).thenReturn(3L)

        StepVerifier.create(shopService.getAllShops(2, 2))
            .expectNextMatches { response ->
                response.data.size == 2 &&
                response.page == 2 &&
                response.pageSize == 2 &&
                response.totalElements == 5L &&
                response.totalPages == 3 &&
                response.data[0].id == 3L &&
                response.data[1].id == 4L
            }
            .verifyComplete()
    }

    @Test
    fun `getShopById should return shop with seller info and products count`() {
        whenever(shopRepository.findById(shopId)).thenReturn(Mono.just(testShop))
        whenever(userService.getUserById(sellerId)).thenReturn(Mono.just(testUserResponse))
        whenever(productServiceClient.countProductsByShopId(shopId)).thenReturn(10L)

        StepVerifier.create(shopService.getShopById(shopId))
            .expectNextMatches { response ->
                response.id == shopId &&
                response.name == "Test Shop" &&
                response.description == "Test Description" &&
                response.sellerId == sellerId &&
                response.sellerName == "John Doe" &&
                response.productsCount == 10L
            }
            .verifyComplete()

        verify(shopRepository).findById(shopId)
        verify(userService).getUserById(sellerId)
        verify(productServiceClient).countProductsByShopId(shopId)
    }

    @Test
    fun `getShopById should throw ResourceNotFoundException when shop not found`() {
        whenever(shopRepository.findById(shopId)).thenReturn(Mono.empty())

        StepVerifier.create(shopService.getShopById(shopId))
            .expectErrorMatches { error ->
                error is ResourceNotFoundException &&
                error.message == "Магазин с ID $shopId не найден"
            }
            .verify()

        verify(shopRepository).findById(shopId)
        verifyNoInteractions(userService, productServiceClient)
    }

    @Test
    fun `getShopProducts should return paginated products when shop exists`() {
        val product = ProductResponse(
            id = 1L,
            name = "Test Product",
            description = "Description",
            price = BigDecimal("99.99"),
            imageUrl = "https://example.com/product.jpg",
            shopId = shopId,
            sellerId = sellerId,
            status = "APPROVED",
            rejectionReason = null,
            averageRating = 4.5,
            commentsCount = 10,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val paginatedResponse = PaginatedResponse(
            data = listOf(product),
            page = 1,
            pageSize = 10,
            totalElements = 1,
            totalPages = 1
        )

        whenever(shopRepository.existsById(shopId)).thenReturn(Mono.just(true))
        whenever(productServiceClient.getProductsByShopId(shopId, 1, 10)).thenReturn(paginatedResponse)

        StepVerifier.create(shopService.getShopProducts(shopId, 1, 10))
            .expectNextMatches { response ->
                response.data.size == 1 &&
                response.data[0].id == 1L &&
                response.totalElements == 1L
            }
            .verifyComplete()

        verify(shopRepository).existsById(shopId)
        verify(productServiceClient).getProductsByShopId(shopId, 1, 10)
    }

    @Test
    fun `getShopProducts should throw ResourceNotFoundException when shop not found`() {
        whenever(shopRepository.existsById(shopId)).thenReturn(Mono.just(false))

        StepVerifier.create(shopService.getShopProducts(shopId, 1, 10))
            .expectErrorMatches { error ->
                error is ResourceNotFoundException &&
                error.message == "Магазин с ID $shopId не найден"
            }
            .verify()

        verify(shopRepository).existsById(shopId)
        verifyNoInteractions(productServiceClient)
    }

    @Test
    fun `getShopProductsFallback should throw ServiceUnavailableException`() {
        val exception = RuntimeException("Product service is down")

        val thrown = org.junit.jupiter.api.assertThrows<ServiceUnavailableException> {
            shopService.getShopProductsFallback(shopId, 1, 10, exception)
        }

        org.junit.jupiter.api.Assertions.assertEquals(
            "Product service is temporarily unavailable. Please try again later.",
            thrown.message
        )
    }

    @Test
    fun `createShop should create shop successfully`() {
        val name = "New Shop"
        val description = "New Description"
        val avatarUrl = "https://example.com/new-avatar.jpg"
        val savedShop = testShop.copy(name = name, description = description, avatarUrl = avatarUrl)

        whenever(shopRepository.existsBySellerId(sellerId)).thenReturn(Mono.just(false))
        whenever(shopRepository.save(any())).thenReturn(Mono.just(savedShop))
        whenever(userService.getUserById(sellerId)).thenReturn(Mono.just(testUserResponse))
        whenever(productServiceClient.countProductsByShopId(any())).thenReturn(0L)

        StepVerifier.create(shopService.createShop(sellerId, name, description, avatarUrl))
            .expectNextMatches { response ->
                response.name == name &&
                response.description == description &&
                response.avatarUrl == avatarUrl &&
                response.sellerId == sellerId
            }
            .verifyComplete()

        verify(shopRepository).existsBySellerId(sellerId)
        verify(shopRepository).save(any())
    }

    @Test
    fun `createShop should throw ConflictException when seller already has a shop`() {
        whenever(shopRepository.existsBySellerId(sellerId)).thenReturn(Mono.just(true))

        StepVerifier.create(shopService.createShop(sellerId, "Shop Name", null, null))
            .expectErrorMatches { error ->
                error is ConflictException &&
                error.message == "Вы уже создали магазин. Один продавец может иметь только один магазин"
            }
            .verify()

        verify(shopRepository).existsBySellerId(sellerId)
        verify(shopRepository, never()).save(any())
    }

    @Test
    fun `createShop should throw IllegalArgumentException when name is blank`() {
        StepVerifier.create(shopService.createShop(sellerId, "", null, null))
            .expectErrorMatches { error ->
                error is IllegalArgumentException &&
                error.message == "Название магазина не может быть пустым"
            }
            .verify()

        verifyNoInteractions(shopRepository)
    }

    @Test
    fun `updateShop should update shop successfully when user is owner`() {
        val newName = "Updated Shop"
        val newDescription = "Updated Description"
        val updatedShop = testShop.copy(name = newName, description = newDescription)

        whenever(shopRepository.findById(shopId)).thenReturn(Mono.just(testShop))
        whenever(shopRepository.save(any())).thenReturn(Mono.just(updatedShop))
        whenever(userService.getUserById(sellerId)).thenReturn(Mono.just(testUserResponse))
        whenever(productServiceClient.countProductsByShopId(shopId)).thenReturn(5L)

        StepVerifier.create(shopService.updateShop(shopId, sellerId, newName, newDescription, null))
            .expectNextMatches { response ->
                response.name == newName &&
                response.description == newDescription &&
                response.avatarUrl == testShop.avatarUrl
            }
            .verifyComplete()

        verify(shopRepository).findById(shopId)
        verify(shopRepository).save(any())
    }

    @Test
    fun `updateShop should throw ForbiddenException when user is not owner`() {
        val differentUserId = 999L

        whenever(shopRepository.findById(shopId)).thenReturn(Mono.just(testShop))

        StepVerifier.create(shopService.updateShop(shopId, differentUserId, "New Name", null, null))
            .expectErrorMatches { error ->
                error is ForbiddenException &&
                error.message == "У вас нет прав для обновления этого магазина"
            }
            .verify()

        verify(shopRepository).findById(shopId)
        verify(shopRepository, never()).save(any())
    }

    @Test
    fun `updateShop should throw ResourceNotFoundException when shop not found`() {
        whenever(shopRepository.findById(shopId)).thenReturn(Mono.empty())

        StepVerifier.create(shopService.updateShop(shopId, sellerId, "New Name", null, null))
            .expectErrorMatches { error ->
                error is ResourceNotFoundException &&
                error.message == "Магазин с ID $shopId не найден"
            }
            .verify()

        verify(shopRepository).findById(shopId)
        verify(shopRepository, never()).save(any())
    }

    @Test
    fun `deleteShop should delete shop when user is owner`() {
        whenever(shopRepository.findById(shopId)).thenReturn(Mono.just(testShop))
        whenever(userService.getUserById(sellerId)).thenReturn(Mono.just(testUserResponse))
        whenever(shopRepository.deleteById(shopId)).thenReturn(Mono.empty())

        StepVerifier.create(shopService.deleteShop(shopId, sellerId))
            .verifyComplete()

        verify(shopRepository).findById(shopId)
        verify(userService).getUserById(sellerId)
        verify(shopRepository).deleteById(shopId)
    }

    @Test
    fun `deleteShop should delete shop when user is admin`() {
        val adminId = 999L
        val adminResponse = testUserResponse.copy(
            id = adminId,
            roles = setOf("ADMIN")
        )

        whenever(shopRepository.findById(shopId)).thenReturn(Mono.just(testShop))
        whenever(userService.getUserById(adminId)).thenReturn(Mono.just(adminResponse))
        whenever(shopRepository.deleteById(shopId)).thenReturn(Mono.empty())

        StepVerifier.create(shopService.deleteShop(shopId, adminId))
            .verifyComplete()

        verify(shopRepository).findById(shopId)
        verify(userService).getUserById(adminId)
        verify(shopRepository).deleteById(shopId)
    }

    @Test
    fun `deleteShop should throw ForbiddenException when user is neither owner nor admin`() {
        val unauthorizedUserId = 999L
        val unauthorizedUserResponse = testUserResponse.copy(
            id = unauthorizedUserId,
            roles = setOf("USER")
        )

        whenever(shopRepository.findById(shopId)).thenReturn(Mono.just(testShop))
        whenever(userService.getUserById(unauthorizedUserId)).thenReturn(Mono.just(unauthorizedUserResponse))

        StepVerifier.create(shopService.deleteShop(shopId, unauthorizedUserId))
            .expectErrorMatches { error ->
                error is ForbiddenException &&
                error.message == "У вас нет прав для удаления этого магазина"
            }
            .verify()

        verify(shopRepository).findById(shopId)
        verify(userService).getUserById(unauthorizedUserId)
        verify(shopRepository, never()).deleteById(any<Long>())
    }

    @Test
    fun `deleteShop should throw ResourceNotFoundException when shop not found`() {
        whenever(shopRepository.findById(shopId)).thenReturn(Mono.empty())

        StepVerifier.create(shopService.deleteShop(shopId, sellerId))
            .expectErrorMatches { error ->
                error is ResourceNotFoundException &&
                error.message == "Магазин с ID $shopId не найден"
            }
            .verify()

        verify(shopRepository).findById(shopId)
        verifyNoInteractions(userService)
        verify(shopRepository, never()).deleteById(any<Long>())
    }

    @Test
    fun `shopToResponseFallback should throw ServiceUnavailableException`() {
        val exception = RuntimeException("Product service is down")

        val thrown = org.junit.jupiter.api.assertThrows<ServiceUnavailableException> {
            shopService.shopToResponseFallback(testShop, exception)
        }

        org.junit.jupiter.api.Assertions.assertEquals(
            "Product service is temporarily unavailable. Please try again later.",
            thrown.message
        )
    }
}
