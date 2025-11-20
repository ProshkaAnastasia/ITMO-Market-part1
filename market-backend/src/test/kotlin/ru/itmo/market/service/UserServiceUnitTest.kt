package ru.itmo.market.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import ru.itmo.market.exception.ResourceNotFoundException
import ru.itmo.market.model.dto.request.UpdateProfileRequest
import ru.itmo.market.model.entity.User
import ru.itmo.market.model.enums.UserRole
import ru.itmo.market.repository.UserRepository
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class UserServiceUnitTest {

    @Mock
    private lateinit var userRepository: UserRepository

    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userService = UserService(userRepository)
    }

    @Test
    fun `should get user by id successfully`() {
        val userId = 1L
        val user = User(
            id = userId,
            username = "johndoe",
            email = "john@example.com",
            password = "hashed_password",
            firstName = "John",
            lastName = "Doe",
            roles = setOf(UserRole.USER)
        )

        whenever(userRepository.findById(eq(userId))).thenReturn(Optional.of(user))

        val result = userService.getUserById(userId)

        assertNotNull(result)
        assertEquals(userId, result.id)
        assertEquals("johndoe", result.username)

        verify(userRepository, times(1)).findById(userId)
    }

    @Test
    fun `should throw exception when user not found`() {
        val userId = 999L

        whenever(userRepository.findById(eq(userId))).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            userService.getUserById(userId)
        }

        verify(userRepository, times(1)).findById(userId)
    }

    @Test
    fun `should update user profile successfully`() {
        val userId = 1L
        val user = User(
            id = userId,
            username = "johndoe",
            email = "john@example.com",
            password = "hashed_password",
            firstName = "John",
            lastName = "Doe",
            roles = setOf(UserRole.USER),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        whenever(userRepository.findById(eq(userId))).thenReturn(Optional.of(user))

        val userCaptor = argumentCaptor<User>()
        whenever(userRepository.save(userCaptor.capture())).thenAnswer { invocation ->
            val u = invocation.arguments[0] as User
            u.copy(id = userId, firstName = "Jane")
        }

        val result = userService.updateProfile(userId, "jane@example.com", "Jane", "Doe")

        assertNotNull(result)
        assertEquals("Jane", result.firstName)

        verify(userRepository, times(1)).save(any())
    }
}
