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
import ru.itmo.market.exception.ForbiddenException
import ru.itmo.market.exception.ResourceNotFoundException
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

    private fun createUserResponse(userId: Long, username: String, firstName: String, lastName: String) = UserResponse(
        id = userId,
        username = username,
        email = "email@test.com",
        firstName = firstName,
        lastName = lastName,
        roles = emptySet(),
        createdAt = LocalDateTime.now()
    )

    @BeforeEach
    fun setUp() {
        commentService = CommentService(
            commentRepository,
            productService, 
            userService     
        )
    }

    
    
    

    @Test
    fun `getProductComments should return paginated comments and map user info`() {
        
        val page = 1
        val pageSize = 10
        val comment = Comment(
            id = COMMENT_ID, productId = PRODUCT_ID, userId = USER_ID,
            text = "Test Comment", rating = 4,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
        )
        val pageable = PageRequest.of(page - 1, pageSize)
        val commentPage = PageImpl(listOf(comment), pageable, 1)

        val userResponse = createUserResponse(USER_ID, "johndoe", "John", "Doe")

        
        whenever(productService.existsById(PRODUCT_ID)).thenReturn(true)
        whenever(commentRepository.findAllByProductId(eq(PRODUCT_ID), eq(pageable))).thenReturn(commentPage)
        whenever(userService.getUserById(USER_ID)).thenReturn(userResponse) 

        
        val result = commentService.getProductComments(PRODUCT_ID, page, pageSize)

        
        assertEquals(1, result.data.size)
        assertEquals("johndoe", result.data.first().userName)
    }
    
    @Test
    fun `getProductComments should throw ResourceNotFoundException if product not exist`() { 
        
        whenever(productService.existsById(PRODUCT_ID)).thenReturn(false) 

        
        assertThrows<ResourceNotFoundException> {
            commentService.getProductComments(PRODUCT_ID, 1, 10)
        }
    }


    
    
    

    @Test
    fun `createComment should create comment successfully`() {
        
        val text = "Great product!"
        val rating = 5
        val userResponse = createUserResponse(USER_ID, "johndoe", "John", "Doe")

        
        whenever(productService.existsById(PRODUCT_ID)).thenReturn(true)
        whenever(userService.getUserById(USER_ID)).thenReturn(userResponse)

        val commentCaptor = argumentCaptor<Comment>()
        whenever(commentRepository.save(commentCaptor.capture())).thenAnswer { invocation ->
            (invocation.arguments[0] as Comment).copy(id = COMMENT_ID, createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now())
        }

        
        val result = commentService.createComment(PRODUCT_ID, USER_ID, text, rating)

        
        assertNotNull(result)
        assertEquals(COMMENT_ID, result.id)
        assertEquals("johndoe", result.userName)
    }

    @Test
    fun `createComment should throw ResourceNotFoundException if product does not exist`() { 
        
        whenever(productService.existsById(PRODUCT_ID)).thenReturn(false) 

        
        assertThrows<ResourceNotFoundException> {
            commentService.createComment(PRODUCT_ID, USER_ID, "Text", 5)
        }
        verify(commentRepository, never()).save(any())
    }

    
    
    

    @Test
    fun `updateComment should update text and rating successfully`() {
        
        val existingComment = Comment(
            id = COMMENT_ID, productId = PRODUCT_ID, userId = USER_ID,
            text = "Old Text", rating = 1,
            createdAt = LocalDateTime.now().minusDays(1), updatedAt = LocalDateTime.now().minusDays(1)
        )
        val userResponse = createUserResponse(USER_ID, "johndoe", "John", "Doe")

        
        whenever(commentRepository.findByIdAndUserId(COMMENT_ID, USER_ID))
            .thenReturn(Optional.of(existingComment))
        whenever(userService.getUserById(USER_ID)).thenReturn(userResponse)
        
        val commentCaptor = argumentCaptor<Comment>()
        whenever(commentRepository.save(commentCaptor.capture())).thenAnswer { 
            (it.arguments[0] as Comment).copy(updatedAt = LocalDateTime.now()) 
        }

        
        val result = commentService.updateComment(PRODUCT_ID, COMMENT_ID, USER_ID, "New Text", 5)

        
        assertEquals("New Text", result.text)
        assertEquals(5, result.rating)
        assertEquals("johndoe", result.userName) 
    }
    
    @Test
    fun `updateComment should throw ResourceNotFoundException if comment and user mismatch`() { 
        
        whenever(commentRepository.findByIdAndUserId(COMMENT_ID, USER_ID))
            .thenReturn(Optional.empty()) 

        
        assertThrows<ResourceNotFoundException> {
            commentService.updateComment(PRODUCT_ID, COMMENT_ID, USER_ID, "New", 5)
        }
        verify(commentRepository, never()).save(any())
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
    }
    
    @Test
    fun `updateComment should throw ResourceNotFoundException if user mapping fails`() {
        
        val existingComment = Comment(
            id = COMMENT_ID, productId = PRODUCT_ID, userId = USER_ID,
            text = "Old Text", rating = 1,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
        )

        
        whenever(commentRepository.findByIdAndUserId(COMMENT_ID, USER_ID))
            .thenReturn(Optional.of(existingComment))

      
            doAnswer { 
            it.arguments[0] as Comment 
            }.whenever(commentRepository).save(any())

        
        whenever(userService.getUserById(USER_ID))
            .thenThrow(ResourceNotFoundException("Пользователь не найден"))

        
        assertThrows<ResourceNotFoundException> {
            commentService.updateComment(PRODUCT_ID, COMMENT_ID, USER_ID, "New Text", 5)
        }
    }


 @Test
    fun `updateComment should throw if user lookup fails`() {
        val existing = Comment(
            id = COMMENT_ID,
            productId = PRODUCT_ID,
            userId = USER_ID,
            text = "Old",
            rating = 3,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        whenever(commentRepository.findByIdAndUserId(COMMENT_ID, USER_ID))
            .thenReturn(Optional.of(existing))

        
        
        

        doAnswer { 
            it.getArgument<Comment>(0)
        }.whenever(commentRepository).save(any())

        
        whenever(userService.getUserById(USER_ID))
            .thenThrow(ResourceNotFoundException("Пользователь не найден"))

        assertThrows<ResourceNotFoundException> {
            commentService.updateComment(PRODUCT_ID, COMMENT_ID, USER_ID, "New Text", 5)
        }
    }
    
    
    

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
    fun `deleteComment should throw ResourceNotFoundException if comment and user mismatch`() { 
        
        whenever(commentRepository.findByIdAndUserId(COMMENT_ID, USER_ID))
            .thenReturn(Optional.empty()) 

        
        assertThrows<ResourceNotFoundException> {
            commentService.deleteComment(PRODUCT_ID, COMMENT_ID, USER_ID)
        }

        verify(commentRepository, never()).deleteById(any())
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
    }
}