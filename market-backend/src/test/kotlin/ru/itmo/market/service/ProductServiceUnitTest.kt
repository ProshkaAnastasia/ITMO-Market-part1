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
import ru.itmo.market.model.dto.request.UpdateProductRequest
import ru.itmo.market.model.entity.Product
import ru.itmo.market.model.enums.ProductStatus
import ru.itmo.market.repository.CommentRepository
import ru.itmo.market.repository.ProductRepository
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import ru.itmo.market.model.entity.Shop
import ru.itmo.market.repository.ShopRepository
import java.time.LocalDateTime
import java.util.*
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
class ProductServiceUnitTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var shopRepository: ShopRepository

    @Mock
    private lateinit var commentRepository: CommentRepository

    private lateinit var productService: ProductService

    @BeforeEach
    fun setUp() {
        productService = ProductService(
            productRepository,
            shopRepository,
            commentRepository
        )
    }

    @Test
    fun `should get product by id successfully`() {
        val productId = 100L
        
        val product = Product(
            id = productId,
            name = "Test Product",
            description = "Test Description",
            price = BigDecimal("99.99"),
            shopId = 1L,
            sellerId = 1L,
            status = ProductStatus.APPROVED,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        whenever(productRepository.findById(eq(productId)))
            .thenReturn(Optional.of(product))
        // ✅ ИСПРАВЛЕНО: Используйте eq() для конкретного ID
        whenever(commentRepository.getAverageRatingByProductId(eq(productId)))
            .thenReturn(4.5)
        whenever(commentRepository.getCommentCountByProductId(eq(productId)))
            .thenReturn(10L)

        val result = productService.getProductById(productId)

        assertNotNull(result)
        assertEquals(productId, result.id)
        assertEquals("Test Product", result.name)
        assertEquals(BigDecimal("99.99"), result.price)
        
        verify(productRepository, times(1)).findById(productId)
        verify(commentRepository, times(1)).getAverageRatingByProductId(productId)
        verify(commentRepository, times(1)).getCommentCountByProductId(productId)
    }

    @Test
    fun `should throw ResourceNotFoundException when product not found`() {
        val productId = 999L
        
        whenever(productRepository.findById(eq(productId)))
            .thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            productService.getProductById(productId)
        }
        
        verify(commentRepository, never()).getAverageRatingByProductId(any())
    }

    @Test
    fun `should create product successfully`() {
        val name = "New Product"
        val description = "Product Description"
        val price = BigDecimal("149.99")
        val imageUrl = "http://example.com/image.jpg"
        val shopId = 1L
        val sellerId = 1L

        val shop = Shop(
            name = "Test Shop",
            description = "Shop Desc",
            avatarUrl = "http://example.url.png",
            sellerId = sellerId
        )

        whenever(shopRepository.findById(any()))
            .thenReturn(Optional.of(shop))

        // ✅ ДЕЙСТВИТЕЛЬНО ИСПРАВЛЕНО: argumentCaptor для захвата аргумента
        val productCaptor = argumentCaptor<Product>()
        whenever(productRepository.save(productCaptor.capture())).thenAnswer { invocation ->
            // Возвращаем сохраненный Product с ID
            (invocation.arguments[0] as Product).copy(id = 1L)
        }
        
        whenever(commentRepository.getAverageRatingByProductId(eq(1L)))
            .thenReturn(null)
        whenever(commentRepository.getCommentCountByProductId(eq(1L)))
            .thenReturn(0L)

        val result = productService.createProduct(name, description, price, imageUrl, shopId, sellerId)

        assertNotNull(result, "ProductResponse не должен быть null")
        assertEquals(name, result.name)
        assertEquals(description, result.description)
        assertEquals(price, result.price)
        assertEquals(imageUrl, result.imageUrl)
        assertEquals(ProductStatus.PENDING.name, result.status)
        
        verify(productRepository, times(1)).save(any())
    }


    @Test
    fun `should create product without image url`() {
        val name = "Product Without Image"
        val description = "Description"
        val price = BigDecimal("99.99")
        val shopId = 1L
        val sellerId = 1L

        val shop = Shop(
            name = "Test Shop",
            description = "Shop Desc",
            avatarUrl = null,
            sellerId = sellerId
        )

        whenever(shopRepository.findById(any()))
         .thenReturn(Optional.of(shop))

        // ✅ ИСПРАВЛЕНО: Используйте argumentCaptor + thenAnswer
        val productCaptor = argumentCaptor<Product>()
        whenever(productRepository.save(productCaptor.capture())).thenAnswer { invocation ->
            val product = invocation.arguments[0] as Product
            product.copy(id = 1L)
        }
        
        whenever(commentRepository.getAverageRatingByProductId(eq(1L)))
            .thenReturn(null)
        whenever(commentRepository.getCommentCountByProductId(eq(1L)))
            .thenReturn(0L)

        val result = productService.createProduct(name, description, price, null, shopId, sellerId)

        assertNotNull(result)
        assertEquals(name, result.name)
        assertEquals(description, result.description)
        assertEquals(price, result.price)
        assertNull(result.imageUrl)  // ✅ Проверяем что imageUrl остается null
        assertEquals(ProductStatus.PENDING.name, result.status)
        
        verify(productRepository, times(1)).save(any())
    }


    @Test
    fun `should update product by seller successfully`() {
        val productId = 100L
        val userId = 1L
        val userRoles = setOf("SELLER")

        val existingProduct = Product(
            id = productId,
            name = "Old Name",
            description = "Old Description",
            price = BigDecimal("99.99"),
            shopId = 1L,
            sellerId = userId,
            status = ProductStatus.APPROVED,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val updateRequest = UpdateProductRequest(
            name = "New Name",
            description = "New Description",
            price = BigDecimal("149.99"),
            imageUrl = null
        )

        // ✅ ИСПРАВЛЕНО: Используйте argumentCaptor + thenAnswer для save()
        val productCaptor = argumentCaptor<Product>()
        whenever(productRepository.findById(eq(productId)))
            .thenReturn(Optional.of(existingProduct))
        whenever(productRepository.save(productCaptor.capture())).thenAnswer { invocation ->
            val product = invocation.arguments[0] as Product
            product.copy(id = productId)  // Возвращаем с оригинальным ID
        }
        
        whenever(commentRepository.getAverageRatingByProductId(eq(productId)))
            .thenReturn(4.5)
        whenever(commentRepository.getCommentCountByProductId(eq(productId)))
            .thenReturn(10L)

        val result = productService.updateProduct(productId, userId, userRoles, updateRequest)

        assertNotNull(result)
        assertEquals("New Name", result.name)
        assertEquals("New Description", result.description)
        assertEquals(BigDecimal("149.99"), result.price)
        assertEquals(ProductStatus.APPROVED.name, result.status)  // ✅ Добавлена проверка статуса
        
        verify(productRepository, times(1)).findById(productId)
        verify(productRepository, times(1)).save(any())
    }


    @Test
    fun `should update product by moderator even if not seller`() {
        val productId = 100L
        val userId = 2L  // Не продавец
        val userRoles = setOf("MODERATOR")

        val existingProduct = Product(
            id = productId,
            name = "Old Name",
            description = "Old Description",
            price = BigDecimal("99.99"),
            shopId = 1L,
            sellerId = 1L,  // Другой продавец
            status = ProductStatus.APPROVED,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val updateRequest = UpdateProductRequest(
            name = "New Name",
            description = null,
            price = null,
            imageUrl = null
        )

        // ✅ ИСПРАВЛЕНО: Используйте argumentCaptor + thenAnswer для save()
        val productCaptor = argumentCaptor<Product>()
        whenever(productRepository.findById(eq(productId)))
            .thenReturn(Optional.of(existingProduct))
        whenever(productRepository.save(productCaptor.capture())).thenAnswer { invocation ->
            val product = invocation.arguments[0] as Product
            product.copy(id = productId)  // Возвращаем с оригинальным ID
        }
        
        whenever(commentRepository.getAverageRatingByProductId(eq(productId)))
            .thenReturn(4.5)
        whenever(commentRepository.getCommentCountByProductId(eq(productId)))
            .thenReturn(10L)

        val result = productService.updateProduct(productId, userId, userRoles, updateRequest)

        assertNotNull(result)
        assertEquals("New Name", result.name)
        assertEquals("Old Description", result.description)  // ✅ Описание не изменилось
        assertEquals(BigDecimal("99.99"), result.price)      // ✅ Цена не изменилась
        assertEquals(ProductStatus.APPROVED.name, result.status)  // ✅ Статус не изменился
        
        verify(productRepository, times(1)).findById(productId)
        verify(productRepository, times(1)).save(any())
    }


    @Test
    fun `should throw ForbiddenException when user is not seller or moderator`() {
        val productId = 100L
        val userId = 2L  // Не продавец
        val userRoles = setOf("USER")  // Обычный пользователь

        val existingProduct = Product(
            id = productId,
            name = "Old Name",
            description = "Old Description",
            price = BigDecimal("99.99"),
            shopId = 1L,
            sellerId = 1L,  // Другой продавец
            status = ProductStatus.APPROVED,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val updateRequest = UpdateProductRequest(
            name = "New Name",
            description = null,
            price = null,
            imageUrl = null
        )

        whenever(productRepository.findById(eq(productId)))
            .thenReturn(Optional.of(existingProduct))

        assertThrows<ForbiddenException> {
            productService.updateProduct(productId, userId, userRoles, updateRequest)
        }
        
        verify(productRepository, never()).save(any())
    }

    @Test
    fun `should throw ResourceNotFoundException when updating non-existent product`() {
        val productId = 999L
        val userId = 1L
        val userRoles = setOf("SELLER")

        val updateRequest = UpdateProductRequest(
            name = "New Name",
            description = null,
            price = null,
            imageUrl = null
        )

        whenever(productRepository.findById(eq(productId)))
            .thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            productService.updateProduct(productId, userId, userRoles, updateRequest)
        }
        
        verify(productRepository, never()).save(any())
    }

    @Test
    fun `should delete product by seller successfully`() {
        val productId = 100L
        val userId = 1L
        val userRoles = setOf("SELLER")

        val product = Product(
            id = productId,
            name = "Product",
            description = "Description",
            price = BigDecimal("99.99"),
            shopId = 1L,
            sellerId = userId,
            status = ProductStatus.APPROVED,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        whenever(productRepository.findById(eq(productId)))
            .thenReturn(Optional.of(product))

        productService.deleteProduct(productId, userId, userRoles)

        verify(productRepository, times(1)).findById(productId)
        verify(productRepository, times(1)).deleteById(productId)
    }

    @Test
    fun `should delete product by moderator even if not seller`() {
        val productId = 100L
        val userId = 2L  // Не продавец
        val userRoles = setOf("MODERATOR")

        val product = Product(
            id = productId,
            name = "Product",
            description = "Description",
            price = BigDecimal("99.99"),
            shopId = 1L,
            sellerId = 1L,  // Другой продавец
            status = ProductStatus.APPROVED,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        whenever(productRepository.findById(eq(productId)))
            .thenReturn(Optional.of(product))

        productService.deleteProduct(productId, userId, userRoles)

        verify(productRepository, times(1)).deleteById(productId)
    }

    @Test
    fun `should throw ForbiddenException when deleting product without permission`() {
        val productId = 100L
        val userId = 2L  // Не продавец
        val userRoles = setOf("USER")  // Обычный пользователь

        val product = Product(
            id = productId,
            name = "Product",
            description = "Description",
            price = BigDecimal("99.99"),
            shopId = 1L,
            sellerId = 1L,  // Другой продавец
            status = ProductStatus.APPROVED,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        whenever(productRepository.findById(eq(productId)))
            .thenReturn(Optional.of(product))

        assertThrows<ForbiddenException> {
            productService.deleteProduct(productId, userId, userRoles)
        }
        
        verify(productRepository, never()).deleteById(any())
    }

    @Test
    fun `should throw ResourceNotFoundException when deleting non-existent product`() {
        val productId = 999L
        val userId = 1L
        val userRoles = setOf("SELLER")

        whenever(productRepository.findById(eq(productId)))
            .thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            productService.deleteProduct(productId, userId, userRoles)
        }
        
        verify(productRepository, never()).deleteById(any())
    }

    @Test
    fun `should get approved products with pagination`() {
        val page = 1
        val pageSize = 10

        val products = listOf(
            Product(
                id = 1L,
                name = "Product 1",
                description = "Description 1",
                price = BigDecimal("50.00"),
                shopId = 1L,
                sellerId = 1L,
                status = ProductStatus.APPROVED,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            ),
            Product(
                id = 2L,
                name = "Product 2",
                description = "Description 2",
                price = BigDecimal("75.00"),
                shopId = 1L,
                sellerId = 1L,
                status = ProductStatus.APPROVED,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        val pageable = PageRequest.of(page - 1, pageSize)
        val productPage = PageImpl(products, pageable, 2)

        whenever(productRepository.findAllByStatus(eq(ProductStatus.APPROVED), eq(pageable)))
            .thenReturn(productPage)
        // ✅ ИСПРАВЛЕНО: Используйте eq() для конкретных ID
        whenever(commentRepository.getAverageRatingByProductId(eq(1L)))
            .thenReturn(4.0)
        whenever(commentRepository.getCommentCountByProductId(eq(1L)))
            .thenReturn(5L)
        whenever(commentRepository.getAverageRatingByProductId(eq(2L)))
            .thenReturn(4.0)
        whenever(commentRepository.getCommentCountByProductId(eq(2L)))
            .thenReturn(5L)

        val result = productService.getApprovedProducts(page, pageSize)

        assertNotNull(result)
        assertEquals(2, result.data.size)
        assertEquals(page, result.page)
        assertEquals(pageSize, result.pageSize)
        assertEquals(2L, result.totalElements)
        assertEquals(1, result.totalPages)
        
        verify(productRepository, times(1)).findAllByStatus(ProductStatus.APPROVED, pageable)
    }
}