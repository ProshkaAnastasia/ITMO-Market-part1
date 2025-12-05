package ru.itmo.market.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import ru.itmo.market.exception.ForbiddenException // Added
import ru.itmo.market.exception.ResourceNotFoundException
import ru.itmo.market.model.entity.Comment
import ru.itmo.market.model.entity.Product
import ru.itmo.market.model.entity.User
import ru.itmo.market.model.enums.ProductStatus
import ru.itmo.market.model.enums.UserRole
import ru.itmo.market.repository.CommentRepository
import ru.itmo.market.repository.ProductRepository
import ru.itmo.market.repository.UserRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class CommentServiceUnitTest {

    @Mock
    private lateinit var commentRepository: CommentRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var userRepository: UserRepository

    private lateinit var commentService: CommentService

    // Common test data
    private val USER_ID = 1L
    private val PRODUCT_ID = 100L
    private val COMMENT_ID = 50L

    @BeforeEach
    fun setUp() {
        commentService = CommentService(
            commentRepository,
            productRepository,
            userRepository
        )
    }

    // ... [Keep your existing createComment tests here] ...
    // I am adding the NEW tests below to fix the RED lines

    // ==========================================
    // 1. Tests for updateComment
    // ==========================================

    @Test
    fun `updateComment should update text and rating successfully`() {
        // 1. Existing Comment
        val existingComment = Comment(
            id = COMMENT_ID, productId = PRODUCT_ID, userId = USER_ID,
            text = "Old Text", rating = 1,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
        )

        // 2. User (needed for response construction)
        val user = User(
            id = USER_ID, username = "u", email = "e", password = "p",
            firstName = "John", lastName = "Doe", roles = emptySet()
        )

        // 3. Mocks
        whenever(commentRepository.findByIdAndUserId(COMMENT_ID, USER_ID))
            .thenReturn(Optional.of(existingComment))

        whenever(userRepository.findById(USER_ID)).thenReturn(Optional.of(user))

        // Capture the saved comment to verify updates
        val commentCaptor = argumentCaptor<Comment>()
        whenever(commentRepository.save(commentCaptor.capture())).thenAnswer { it.arguments[0] as Comment }

        // Act
        val result = commentService.updateComment(PRODUCT_ID, COMMENT_ID, USER_ID, "New Text", 5)

        // Assert
        assertEquals("New Text", result.text)
        assertEquals(5, result.rating)
        assertEquals("John Doe", result.userName) // Covers response mapping
        
        // Verify the object sent to DB
        val saved = commentCaptor.firstValue
        assertEquals("New Text", saved.text)
        assertEquals(5, saved.rating)
    }

    @Test
    fun `updateComment should throw ForbiddenException if product id does not match`() {
        val wrongProductId = 999L
        val existingComment = Comment(
            id = COMMENT_ID, productId = PRODUCT_ID, userId = USER_ID,
            text = "Text", rating = 5,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
        )

        whenever(commentRepository.findByIdAndUserId(COMMENT_ID, USER_ID))
            .thenReturn(Optional.of(existingComment))

        val ex = assertThrows<ForbiddenException> {
            commentService.updateComment(wrongProductId, COMMENT_ID, USER_ID, "Text", 5)
        }

        assertEquals("Этот комментарий не относится к данному товару", ex.message)
        verify(commentRepository, never()).save(any())
    }

    // @Test
    // @Disabled
    // fun `updateComment should throw exception if user not found during response creation`() {
    //     // This specifically targets the RED line in the return statement
    //     val existingComment = Comment(
    //         id = COMMENT_ID, productId = PRODUCT_ID, userId = USER_ID,
    //         text = "Old", rating = 1, createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
    //     )

    //     whenever(commentRepository.findByIdAndUserId(COMMENT_ID, USER_ID))
    //         .thenReturn(Optional.of(existingComment))
        
    //     whenever(commentRepository.save(any())).thenReturn(existingComment)
        
    //     // Make user lookup fail
    //     whenever(userRepository.findById(USER_ID)).thenReturn(Optional.empty())

    //     assertThrows<ResourceNotFoundException> {
    //         commentService.updateComment(PRODUCT_ID, COMMENT_ID, USER_ID, "New", 5)
    //     }
    // }

    // ==========================================
    // 2. Tests for deleteComment
    // ==========================================

    @Test
    fun `deleteComment should delete successfully`() {
        val existingComment = Comment(
            id = COMMENT_ID, productId = PRODUCT_ID, userId = USER_ID,
            text = "Text", rating = 5, createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
        )

        whenever(commentRepository.findByIdAndUserId(COMMENT_ID, USER_ID))
            .thenReturn(Optional.of(existingComment))

        commentService.deleteComment(PRODUCT_ID, COMMENT_ID, USER_ID)

        verify(commentRepository).deleteById(COMMENT_ID)
    }

    @Test
    fun `deleteComment should throw ForbiddenException if product id does not match`() {
        val wrongProductId = 999L
        val existingComment = Comment(
            id = COMMENT_ID, productId = PRODUCT_ID, userId = USER_ID,
            text = "Text", rating = 5, createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
        )

        whenever(commentRepository.findByIdAndUserId(COMMENT_ID, USER_ID))
            .thenReturn(Optional.of(existingComment))

        val ex = assertThrows<ForbiddenException> {
            commentService.deleteComment(wrongProductId, COMMENT_ID, USER_ID)
        }
        
        assertEquals("Этот комментарий не относится к данному товару", ex.message)
        verify(commentRepository, never()).deleteById(any())
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
    // ==========================================
    // 3. Tests for getProductComments (The mapping logic)
    // ==========================================

    // @Test
    // @Disabled
    // fun `getProductComments should map user details correctly inside the page`() {
    //     // This targets the RED map block
        
    //     val comment = Comment(
    //         id = COMMENT_ID, productId = PRODUCT_ID, userId = USER_ID,
    //         text = "Text", rating = 5, createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
    //     )
        
    //     val user = User(
    //         id = USER_ID, username = "u", email = "e", password = "p",
    //         firstName = "John", lastName = "Doe", roles = emptySet()
    //     )

    //     val pageRequest = PageRequest.of(0, 10)
    //     val commentPage = PageImpl(listOf(comment), pageRequest, 1)

    //     // Assuming your service calls findAllByProductId inside getProductComments
    //     whenever(commentRepository.findAllByProductId(eq(PRODUCT_ID), any()))
    //         .thenReturn(commentPage)
            
    //     whenever(userRepository.findById(USER_ID)).thenReturn(Optional.of(user))

    //     // Act
    //     val result = commentService.getProductComments(PRODUCT_ID, 1, 10)

    //     // Assert
    //     assertEquals(1, result.data.size)
    //     val responseItem = result.data[0]
    //     assertEquals("John Doe", responseItem.userName)
    //     assertEquals("Text", responseItem.text)
        
    //     verify(userRepository).findById(USER_ID)
    // }
}