package ru.itmo.market.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.delete
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.containers.PostgreSQLContainer
import ru.itmo.market.repository.*
import ru.itmo.market.model.entity.Shop
import ru.itmo.market.model.entity.Product
import ru.itmo.market.model.dto.request.CreateProductRequest
import ru.itmo.market.model.dto.request.UpdateProductRequest
import ru.itmo.market.model.enums.UserRole
import ru.itmo.market.model.enums.ProductStatus
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class ProductControllerIntegrationTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:15").apply {
            withDatabaseName("itmo_market")
            withUsername("itmo_user")
            withPassword("itmo_password")
        }
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var shopRepository: ShopRepository

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @BeforeEach
    fun setUp() {
        productRepository.deleteAll()
        shopRepository.deleteAll()
        userRepository.deleteAll()
    }

    

    @Test
    fun `should get empty products list`() {
        mockMvc.get("/api/products") {
            param("page", "1")
            param("pageSize", "20")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data") { isArray() }
            jsonPath("$.data.length()") { value(0) }
        }
    }

    @Test
    fun `should get products with pagination`() {
        val seller = testAuthHelper.createTestUser(username = "seller", email = "seller@example.com", roles = setOf(UserRole.SELLER))
        val shop = shopRepository.save(
            Shop(
                name = "Test Shop",
                description = "Shop Desc",
                avatarUrl = null,
                sellerId = seller.id
            )
        )

        repeat(3) { index ->
            productRepository.save(
                Product(
                    name = "Product $index",
                    description = "Desc",
                    price = BigDecimal("100.00"),
                    imageUrl = null,
                    shopId = shop.id,
                    sellerId = seller.id,
                    status = ProductStatus.APPROVED
                )
            )
        }

        mockMvc.get("/api/products") {
            param("page", "1")
            param("pageSize", "20")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data") { isArray() }
            jsonPath("$.data.length()") { value(3) }
            jsonPath("$.page") { value(1) }
            jsonPath("$.pageSize") { value(20) }
        }
    }

    @Test
    fun `should return 400 for invalid pagination parameters`() {
        mockMvc.get("/api/products") {
            param("page", "0")
            param("pageSize", "20")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    

    @Test
    fun `should get products for infinite scroll`() {
        val seller = testAuthHelper.createTestUser(username = "seller", email = "seller@example.com", roles = setOf(UserRole.SELLER))
        val shop = shopRepository.save(
            Shop(
                name = "Test Shop",
                description = "Shop Desc",
                avatarUrl = null,
                sellerId = seller.id
            )
        )

        repeat(5) { index ->
            productRepository.save(
                Product(
                    name = "Product $index",
                    description = "Desc",
                    price = BigDecimal("100.00"),
                    imageUrl = null,
                    shopId = shop.id,
                    sellerId = seller.id,
                    status = ProductStatus.APPROVED
                )
            )
        }

        mockMvc.get("/api/products/infinite") {
            param("page", "1")
            param("pageSize", "20")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data") { isArray() }
            jsonPath("$.data.length()") { value(5) }
            jsonPath("$.page") { value(1) }
            jsonPath("$.pageSize") { value(20) }
            jsonPath("$.hasMore") { value(false) }
        }
    }

    @Test
    fun `should get products with hasMore flag`() {
        val seller = testAuthHelper.createTestUser(username = "seller", email = "seller@example.com", roles = setOf(UserRole.SELLER))
        val shop = shopRepository.save(
            Shop(
                name = "Test Shop",
                description = "Shop Desc",
                avatarUrl = null,
                sellerId = seller.id
            )
        )

        repeat(25) { index ->
            productRepository.save(
                Product(
                    name = "Product $index",
                    description = "Desc",
                    price = BigDecimal("100.00"),
                    imageUrl = null,
                    shopId = shop.id,
                    sellerId = seller.id,
                    status = ProductStatus.APPROVED
                )
            )
        }

        mockMvc.get("/api/products/infinite") {
            param("page", "1")
            param("pageSize", "20")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.length()") { value(20) }
            jsonPath("$.page") { value(1) }
            jsonPath("$.pageSize") { value(20) }
            jsonPath("$.hasMore") { value(true) }
        }
    }

    

    @Test
    fun `should search products by keywords`() {
        val seller = testAuthHelper.createTestUser(username = "seller", email = "seller@example.com", roles = setOf(UserRole.SELLER))
        val shop = shopRepository.save(
            Shop(
                name = "Test Shop",
                description = "Shop Desc",
                avatarUrl = null,
                sellerId = seller.id
            )
        )

        productRepository.save(
            Product(
                name = "Laptop Dell XPS",
                description = "High performance laptop",
                price = BigDecimal("1000.00"),
                imageUrl = null,
                shopId = shop.id,
                sellerId = seller.id,
                status = ProductStatus.APPROVED
            )
        )
        productRepository.save(
            Product(
                name = "Mouse Logitech",
                description = "Wireless mouse",
                price = BigDecimal("50.00"),
                imageUrl = null,
                shopId = shop.id,
                sellerId = seller.id,
                status = ProductStatus.APPROVED
            )
        )

        mockMvc.get("/api/products/search") {
            param("keywords", "laptop")
            param("page", "1")
            param("pageSize", "20")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data") { isArray() }
            jsonPath("$.data.length()") { value(1) }
            jsonPath("$.data[0].name") { value("Laptop Dell XPS") }
            jsonPath("$.data[0].price") { value(1000.00) }
            jsonPath("$.data[0].sellerId") { value(seller.id.toInt()) }
            jsonPath("$.data[0].status") { value("APPROVED") }
        }
    }

    @Test 
    fun `should return 400 for empty keywords`() {
        mockMvc.get("/api/products/search") {
            param("keywords", "")
            param("page", "1")
            param("pageSize", "20")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    

    @Test
    fun `should get product by id`() {
        val seller = testAuthHelper.createTestUser(username = "seller", email = "seller@example.com", roles = setOf(UserRole.SELLER))
        val shop = shopRepository.save(
            Shop(
                name = "Test Shop",
                description = "Shop Desc",
                avatarUrl = null,
                sellerId = seller.id
            )
        )
        val product = productRepository.save(
            Product(
                name = "Test Product",
                description = "Desc",
                price = BigDecimal("100.00"),
                imageUrl = null,
                shopId = shop.id,
                sellerId = seller.id,
                status = ProductStatus.APPROVED
            )
        )

        mockMvc.get("/api/products/${product.id}").andExpect {
            status { isOk() }
            jsonPath("$.id") { value(product.id.toInt()) }
            jsonPath("$.name") { value("Test Product") }
            jsonPath("$.price") { value(100.00) }
        }
    }

    @Test
    fun `should return 404 for non-existent product`() {
        mockMvc.get("/api/products/99999").andExpect {
            status { isNotFound() }
        }
    }

    

    @Test
    fun `should create product successfully`() {
        val seller = testAuthHelper.createTestUser(username = "seller", email = "seller@example.com", roles = setOf(UserRole.SELLER))
        
        val shop = shopRepository.save(
            Shop(
                name = "Test Shop",
                description = "Shop Desc",
                avatarUrl = null,
                sellerId = seller.id
            )
        )

        val createRequest = CreateProductRequest(
            name = "New Product",
            description = "Product Desc",
            price = BigDecimal("150.00"),
            imageUrl = null,
            shopId = shop.id
        )

        mockMvc.post("/api/products") {
            param("sellerId", seller.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.name") { value("New Product") }
            jsonPath("$.price") { value(150.00) }
            jsonPath("$.status") { value("PENDING") }
        }
    }

    @Test
    fun `should return 400 for empty product name`() {
        val seller = testAuthHelper.createTestUser(username = "seller", email = "seller@example.com", roles = setOf(UserRole.SELLER))
        
        val shop = shopRepository.save(
            Shop(
                name = "Test Shop",
                description = "Shop Desc",
                avatarUrl = null,
                sellerId = seller.id
            )
        )

        val createRequest = CreateProductRequest(
            name = "",
            description = "Product Desc",
            price = BigDecimal("150.00"),
            imageUrl = null,
            shopId = shop.id
        )

        mockMvc.post("/api/products") {
            param("sellerId", seller.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test 
    fun `should return 404 when shop not found`() {
        val seller = testAuthHelper.createTestUser(username = "seller", email = "seller@example.com", roles = setOf(UserRole.SELLER))
        

        val createRequest = CreateProductRequest(
            name = "New Product",
            description = "Product Desc",
            price = BigDecimal("150.00"),
            imageUrl = null,
            shopId = 99999L
        )

        mockMvc.post("/api/products") {
            param("sellerId", seller.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest)
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    @Disabled
    fun `should return 401 when creating product without authorization`() {
        val createRequest = CreateProductRequest(
            name = "New Product",
            description = "Product Desc",
            price = BigDecimal("150.00"),
            imageUrl = null,
            shopId = 1L
        )

        mockMvc.post("/api/products") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createRequest)
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    

    @Test
    fun `should update product successfully`() {
        val seller = testAuthHelper.createTestUser(username = "seller", email = "seller@example.com", roles = setOf(UserRole.SELLER))
        
        val shop = shopRepository.save(
            Shop(
                name = "Test Shop",
                description = "Shop Desc",
                avatarUrl = null,
                sellerId = seller.id
            )
        )
        val product = productRepository.save(
            Product(
                name = "Test Product",
                description = "Desc",
                price = BigDecimal("100.00"),
                imageUrl = null,
                shopId = shop.id,
                sellerId = seller.id,
                status = ProductStatus.APPROVED
            )
        )

        val updateRequest = UpdateProductRequest(
            name = "Updated Product",
            description = "Updated Desc",
            price = BigDecimal("200.00"),
            imageUrl = null
        )

        mockMvc.put("/api/products/${product.id}") {
            param("sellerId", seller.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("Updated Product") }
            jsonPath("$.price") { value(200.00) }
            jsonPath("$.status") { value("APPROVED") }
        }
    }

    @Test
    fun `should return 403 when updating other user product`() {
        val seller1 = testAuthHelper.createTestUser(username = "seller1", email = "seller1@example.com", roles = setOf(UserRole.SELLER))
        val seller2 = testAuthHelper.createTestUser(username = "seller2", email = "seller2@example.com", roles = setOf(UserRole.SELLER))
        

        val shop = shopRepository.save(
            Shop(
                name = "Test Shop",
                description = "Shop Desc",
                avatarUrl = null,
                sellerId = seller1.id
            )
        )
        val product = productRepository.save(
            Product(
                name = "Test Product",
                description = "Desc",
                price = BigDecimal("100.00"),
                imageUrl = null,
                shopId = shop.id,
                sellerId = seller1.id,
                status = ProductStatus.APPROVED
            )
        )

        val updateRequest = UpdateProductRequest(
            name = "Updated Product",
            description = "Updated Desc",
            price = BigDecimal("200.00"),
            imageUrl = null
        )

        mockMvc.put("/api/products/${product.id}") {
            param("sellerId", seller2.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `should return 404 when updating non-existent product`() {
        val seller = testAuthHelper.createTestUser(username = "seller", email = "seller@example.com", roles = setOf(UserRole.SELLER))
        

        val updateRequest = UpdateProductRequest(
            name = "Updated Product",
            description = "Updated Desc",
            price = BigDecimal("200.00"),
            imageUrl = null
        )

        mockMvc.put("/api/products/99999") {
            param("sellerId", seller.id.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
        }.andExpect {
            status { isNotFound() }
        }
    }

    

    @Test
    fun `should delete product successfully`() {
        val seller = testAuthHelper.createTestUser(username = "seller", email = "seller@example.com", roles = setOf(UserRole.SELLER))
        
        val shop = shopRepository.save(
            Shop(
                name = "Test Shop",
                description = "Shop Desc",
                avatarUrl = null,
                sellerId = seller.id
            )
        )
        val product = productRepository.save(
            Product(
                name = "Test Product",
                description = "Desc",
                price = BigDecimal("100.00"),
                imageUrl = null,
                shopId = shop.id,
                sellerId = seller.id,
                status = ProductStatus.APPROVED
            )
        )

        mockMvc.delete("/api/products/${product.id}") {
            param("sellerId", seller.id.toString())
        }.andExpect {
            status { isNoContent() }
        }

        mockMvc.get("/api/products/${product.id}").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `should return 403 when deleting other user product`() {
        val seller1 = testAuthHelper.createTestUser(username = "seller1", email = "seller1@example.com", roles = setOf(UserRole.SELLER))
        val seller2 = testAuthHelper.createTestUser(username = "seller2", email = "seller2@example.com", roles = setOf(UserRole.SELLER))
        

        val shop = shopRepository.save(
            Shop(
                name = "Test Shop",
                description = "Shop Desc",
                avatarUrl = null,
                sellerId = seller1.id
            )
        )
        val product = productRepository.save(
            Product(
                name = "Test Product",
                description = "Desc",
                price = BigDecimal("100.00"),
                imageUrl = null,
                shopId = shop.id,
                sellerId = seller1.id,
                status = ProductStatus.APPROVED
            )
        )

        mockMvc.delete("/api/products/${product.id}") {
            param("sellerId", seller2.id.toString())
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `should return 404 when deleting non-existent product`() {
        val seller = testAuthHelper.createTestUser(username = "seller", email = "seller@example.com", roles = setOf(UserRole.SELLER))
        

        mockMvc.delete("/api/products/99999") {
            param("sellerId", seller.id.toString())
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    @Disabled
    fun `should return 401 when deleting product without authorization`() {
        mockMvc.delete("/api/products/1").andExpect {
            status { isUnauthorized() }
        }
    }
}