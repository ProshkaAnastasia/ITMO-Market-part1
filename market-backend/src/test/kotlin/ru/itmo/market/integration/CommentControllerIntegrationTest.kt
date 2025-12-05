package ru.itmo.market.integration 
import com.fasterxml.jackson.databind.ObjectMapper 
import org.junit.jupiter.api.BeforeEach 
import org.junit.jupiter.api.Test 
import org.junit.jupiter.api.DisplayName 
import org.mockito.kotlin.eq 
import org.mockito.kotlin.whenever 
import org.springframework.beans.factory.annotation.Autowired 
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc 
import org.springframework.boot.test.context.SpringBootTest 
import org.springframework.http.MediaType 
import org.springframework.test.context.ActiveProfiles 
import org.springframework.test.web.servlet.MockMvc 
import org.springframework.test.web.servlet.get 
import org.springframework.test.web.servlet.post 
import org.testcontainers.junit.jupiter.Container 
import org.testcontainers.junit.jupiter.Testcontainers 
import org.testcontainers.containers.PostgreSQLContainer 
import ru.itmo.market.model.entity.Product 
import ru.itmo.market.model.entity.User 
import ru.itmo.market.model.entity.Shop 
import ru.itmo.market.model.enums.UserRole 
import ru.itmo.market.repository.* 
import java.math.BigDecimal 
import java.util.*

import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.DynamicPropertyRegistry


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Comment Controller Integration Tests")
class CommentControllerIntegrationTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:15").apply {
            withDatabaseName("itmo_market")
            withUsername("itmo_user")
            withPassword("itmo_password")
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureDataSource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto") { "update" }
        }
    }

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var commentRepository: CommentRepository
    @Autowired lateinit var productRepository: ProductRepository
    @Autowired lateinit var shopRepository: ShopRepository
    @Autowired lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        commentRepository.deleteAll()
        productRepository.deleteAll()
        shopRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    @DisplayName("should get product comments with pagination")
    fun testGetProductComments() {
        val seller = userRepository.save(
            User(
                username = "seller",
                email = "seller@example.com",
                password = "hashed",
                firstName = "Test",
                lastName = "Seller",
                roles = setOf(UserRole.SELLER)
            )
        )
        val shop = shopRepository.save(
            Shop(
                name = "Test Shop",
                description = "Shop desc",
                avatarUrl = null,
                sellerId = seller.id
            )
        )
        val product = productRepository.save(
            Product(
                name = "Test_product",
                description = "Desc",
                price = BigDecimal.TEN,
                shopId = shop.id,
                sellerId = seller.id
            )
        )

        mockMvc.get("/api/products/${product.id}/comments") {
            param("page", "1")
            param("pageSize", "20")
        }.andExpect {
            status { isOk() }
            jsonPath("$.page") { value(1) }
            jsonPath("$.pageSize") { value(20) }
        }
    }

    @Test
    @DisplayName("should reject comment request for non-existent product")
    fun testGetCommentsForNonExistentProduct() {
        mockMvc.get("/api/products/999/comments") {
            param("page", "1")
            param("pageSize", "20")
        }.andExpect {
            status { isNotFound() }
        }
    }
}
