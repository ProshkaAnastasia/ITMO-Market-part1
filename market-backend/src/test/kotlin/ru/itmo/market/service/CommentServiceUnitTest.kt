package ru.itmo.market.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import ru.itmo.market.model.entity.User
import ru.itmo.market.model.entity.Product
import ru.itmo.market.model.entity.Comment
import ru.itmo.market.model.enums.UserRole
import ru.itmo.market.model.enums.ProductStatus
import ru.itmo.market.repository.UserRepository
import ru.itmo.market.repository.CommentRepository
import ru.itmo.market.repository.ProductRepository
import ru.itmo.market.exception.ResourceNotFoundException
import java.time.LocalDateTime
import java.util.*
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
class CommentServiceUnitTest {

    @Mock
    private lateinit var commentRepository: CommentRepository
    
    @Mock
    private lateinit var productRepository: ProductRepository
    
    @Mock
    private lateinit var userRepository: UserRepository

    private lateinit var commentService: CommentService

    @BeforeEach
    fun setUp() {
        commentService = CommentService(
            commentRepository,
            productRepository,
            userRepository
        )
    }

    @Test
    fun `should create comment successfully`() {
        val productId = 100L
        val userId = 1L
        val text = "Great product!"
        val rating = 5

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
        // ✅ ИСПРАВЛЕНО: eq() для конкретного ID
        whenever(productRepository.findById(eq(productId)))
            .thenReturn(Optional.of(product))

        val user = User(
            id = userId,
            username = "johndoe",
            email = "john@example.com",
            password = "hashed_password",
            firstName = "John",
            lastName = "Doe",
            roles = setOf(UserRole.USER)
        )
        // ✅ ИСПРАВЛЕНО: eq() для конкретного ID
        whenever(userRepository.findById(eq(userId)))
            .thenReturn(Optional.of(user))

        // ✅ ИСПРАВЛЕНО: Используйте thenAnswer
        val commentCaptor = argumentCaptor<Comment>()
        whenever(commentRepository.save(commentCaptor.capture())).thenAnswer { invocation ->
            val comment = invocation.arguments[0] as Comment
            comment.copy(id = 1L)
        }

        val result = commentService.createComment(productId, userId, text, rating)

        assertNotNull(result, "Comment не должен быть null")
        assertEquals(text, result.text)
        assertEquals(rating, result.rating)
        assertEquals(productId, result.productId)
        assertEquals(userId, result.userId)
        
        verify(commentRepository, times(1)).save(any())
    }

    @Test
    fun `should throw exception when product not found`() {
        val productId = 999L
        val userId = 1L
        val text = "Great product!"
        val rating = 5

        whenever(productRepository.findById(productId)).thenReturn(Optional.empty())

        assertThrows<Exception> {
            commentService.createComment(productId, userId, text, rating)
        }

        verify(commentRepository, never()).save(any())
    }

    @Test
    fun `should throw exception when user not found`() {
        val productId = 100L
        val userId = 999L
        val text = "Great product!"
        val rating = 5

        assertThrows<Exception> {
            commentService.createComment(productId, userId, text, rating)
        }
    }


}
