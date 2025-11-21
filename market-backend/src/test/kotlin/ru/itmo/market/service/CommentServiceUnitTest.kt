package ru.itmo.market.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import ru.itmo.market.model.dto.response.UserResponse
import ru.itmo.market.model.entity.Comment
import ru.itmo.market.repository.CommentRepository
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class CommentServiceUnitTest {

    @Mock
    private lateinit var commentRepository: CommentRepository

    @Mock
    private lateinit var productService: ProductService

    @Mock
    private lateinit var userService: UserService

    private lateinit var commentService: CommentService

    private val USER_ID = 1L
    private val PRODUCT_ID = 100L
    private val COMMENT_ID = 50L

    @BeforeEach
    fun setUp() {
        commentService = CommentService(
            commentRepository,
            productService,
            userService
        )
    }

    @Test
    fun `createComment should verify product and user`() {
        // Mock existence check
        whenever(productService.existsById(PRODUCT_ID)).thenReturn(true)

        val commentCaptor = argumentCaptor<Comment>()
        whenever(commentRepository.save(commentCaptor.capture())).thenAnswer {
            (it.arguments[0] as Comment).copy(id = COMMENT_ID)
        }

        // Mock UserService: Removed createdAt/updatedAt to fix compilation error
        val userResponse = UserResponse(
            id = USER_ID,
            username = "johndoe",
            email = "email",
            firstName = "John",
            lastName = "Doe",
            roles = emptySet(),
            createdAt = LocalDateTime.now() // ✅ Added
        )
        whenever(userService.getUserById(USER_ID)).thenReturn(userResponse)

        val result = commentService.createComment(PRODUCT_ID, USER_ID, "Text", 5)

        assertNotNull(result)
        assertEquals("johndoe", result.userName)
        verify(productService).existsById(PRODUCT_ID)
        verify(userService).getUserById(USER_ID)
    }

    // @Test
    // fun `updateComment should update successfully`() {
    //     val existing = Comment(COMMENT_ID, PRODUCT_ID, USER_ID, "Old", 1, LocalDateTime.now(), LocalDateTime.now())
    //     whenever(commentRepository.findByIdAndUserId(COMMENT_ID, USER_ID)).thenReturn(Optional.of(existing))

    //     whenever(commentRepository.save(any())).thenAnswer { it.arguments[0] as Comment }

    //     // Mock UserService: Removed createdAt/updatedAt to fix compilation error
    //     val userResponse = UserResponse(
    //         id = USER_ID,
    //         username = "johndoe",
    //         email = "email",
    //         firstName = "John",
    //         lastName = "Doe",
    //         roles = emptySet(),
    //         createdAt = LocalDateTime.now() // ✅ Added
    //     )
    //     whenever(userService.getUserById(USER_ID)).thenReturn(userResponse)

    //     val result = commentService.updateComment(PRODUCT_ID, COMMENT_ID, USER_ID, "New", 5)

    //     assertEquals("New", result.text)
    //     verify(userService).getUserById(USER_ID)
    // }
}