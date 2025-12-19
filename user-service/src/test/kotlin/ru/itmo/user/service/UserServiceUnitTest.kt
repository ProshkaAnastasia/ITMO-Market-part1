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
import ru.itmo.user.exception.BadRequestException
import ru.itmo.user.exception.ConflictException
import ru.itmo.user.exception.ResourceNotFoundException
import ru.itmo.user.model.entity.User
import ru.itmo.user.model.enums.UserRole
import ru.itmo.user.repository.UserRepository
import ru.itmo.user.repository.UserRoleRepository
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class UserServiceUnitTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var userRoleRepository: UserRoleRepository

    private lateinit var userService: UserService

    private val testUser = User(
        id = 1L,
        username = "johndoe",
        email = "john@example.com",
        password = "hashed_password",
        firstName = "John",
        lastName = "Doe",
        roles = emptySet(),
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    @BeforeEach
    fun setUp() {
        userService = UserService(userRepository, userRoleRepository)
    }

    @Test
    fun `getUserById should return user with roles when user exists`() {
        val userId = 1L
        val roles = listOf(UserRole.USER.name, UserRole.SELLER.name)

        whenever(userRepository.findById(userId)).thenReturn(Mono.just(testUser))
        whenever(userRoleRepository.findRolesByUserId(userId)).thenReturn(Flux.fromIterable(roles))

        StepVerifier.create(userService.getUserById(userId))
            .expectNextMatches { response ->
                response.id == userId &&
                response.username == "johndoe" &&
                response.email == "john@example.com" &&
                response.firstName == "John" &&
                response.lastName == "Doe" &&
                response.roles == setOf("USER", "SELLER")
            }
            .verifyComplete()

        verify(userRepository).findById(userId)
        verify(userRoleRepository).findRolesByUserId(userId)
    }

    @Test
    fun `getUserById should throw ResourceNotFoundException when user not found`() {
        val userId = 999L

        whenever(userRepository.findById(userId)).thenReturn(Mono.empty())

        StepVerifier.create(userService.getUserById(userId))
            .expectErrorMatches { error ->
                error is ResourceNotFoundException &&
                error.message == "Пользователь $userId не найден"
            }
            .verify()

        verify(userRepository).findById(userId)
        verifyNoInteractions(userRoleRepository)
    }

    @Test
    fun `getCurrentUser should delegate to getUserById`() {
        val userId = 1L
        val roles = listOf(UserRole.USER.name)

        whenever(userRepository.findById(userId)).thenReturn(Mono.just(testUser))
        whenever(userRoleRepository.findRolesByUserId(userId)).thenReturn(Flux.fromIterable(roles))

        StepVerifier.create(userService.getCurrentUser(userId))
            .expectNextMatches { response ->
                response.id == userId &&
                response.username == "johndoe"
            }
            .verifyComplete()

        verify(userRepository).findById(userId)
    }

    @Test
    fun `updateProfile should update email, firstName and lastName successfully`() {
        val userId = 1L
        val newEmail = "jane@example.com"
        val newFirstName = "Jane"
        val newLastName = "Smith"
        val roles = listOf(UserRole.USER.name)

        val updatedUser = testUser.copy(
            email = newEmail,
            firstName = newFirstName,
            lastName = newLastName,
            updatedAt = LocalDateTime.now()
        )

        // First call to findById (in updateProfile), second call (in getUserById after save)
        whenever(userRepository.findById(userId))
            .thenReturn(Mono.just(testUser))
            .thenReturn(Mono.just(updatedUser))
        whenever(userRepository.existsByEmail(newEmail)).thenReturn(Mono.just(false))
        whenever(userRepository.save(any())).thenReturn(Mono.just(updatedUser))
        whenever(userRoleRepository.findRolesByUserId(userId)).thenReturn(Flux.fromIterable(roles))

        StepVerifier.create(userService.updateProfile(userId, newEmail, newFirstName, newLastName))
            .expectNextMatches { response ->
                response.email == newEmail &&
                response.firstName == newFirstName &&
                response.lastName == newLastName
            }
            .verifyComplete()

        verify(userRepository, times(2)).findById(userId)
        verify(userRepository).existsByEmail(newEmail)
        verify(userRepository).save(any())
    }

    @Test
    fun `updateProfile should keep existing email when email is not provided`() {
        val userId = 1L
        val newFirstName = "Jane"
        val roles = listOf(UserRole.USER.name)

        val updatedUser = testUser.copy(firstName = newFirstName)

        // First call to findById (in updateProfile), second call (in getUserById after save)
        whenever(userRepository.findById(userId))
            .thenReturn(Mono.just(testUser))
            .thenReturn(Mono.just(updatedUser))
        whenever(userRepository.save(any())).thenReturn(Mono.just(updatedUser))
        whenever(userRoleRepository.findRolesByUserId(userId)).thenReturn(Flux.fromIterable(roles))

        StepVerifier.create(userService.updateProfile(userId, null, newFirstName, null))
            .expectNextMatches { response ->
                response.email == testUser.email &&
                response.firstName == newFirstName &&
                response.lastName == testUser.lastName
            }
            .verifyComplete()

        verify(userRepository, times(2)).findById(userId)
        verify(userRepository, never()).existsByEmail(any())
        verify(userRepository).save(any())
    }

    @Test
    fun `updateProfile should throw BadRequestException for invalid email format`() {
        val userId = 1L
        val invalidEmail = "invalid-email"

        whenever(userRepository.findById(userId)).thenReturn(Mono.just(testUser))

        StepVerifier.create(userService.updateProfile(userId, invalidEmail, null, null))
            .expectErrorMatches { error ->
                error is BadRequestException &&
                error.message!!.contains("Некорректный формат email")
            }
            .verify()

        verify(userRepository).findById(userId)
        verify(userRepository, never()).existsByEmail(any())
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `updateProfile should throw ConflictException when email already exists`() {
        val userId = 1L
        val existingEmail = "existing@example.com"

        whenever(userRepository.findById(userId)).thenReturn(Mono.just(testUser))
        whenever(userRepository.existsByEmail(existingEmail)).thenReturn(Mono.just(true))

        StepVerifier.create(userService.updateProfile(userId, existingEmail, null, null))
            .expectErrorMatches { error ->
                error is ConflictException &&
                error.message == "Email $existingEmail уже используется"
            }
            .verify()

        verify(userRepository).findById(userId)
        verify(userRepository).existsByEmail(existingEmail)
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `updateProfile should throw ResourceNotFoundException when user not found`() {
        val userId = 999L

        whenever(userRepository.findById(userId)).thenReturn(Mono.empty())

        StepVerifier.create(userService.updateProfile(userId, "new@example.com", "New", "Name"))
            .expectErrorMatches { error ->
                error is ResourceNotFoundException &&
                error.message == "Пользователь не найден"
            }
            .verify()

        verify(userRepository).findById(userId)
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `deleteProfile should delete user when user exists`() {
        val userId = 1L

        whenever(userRepository.findById(userId)).thenReturn(Mono.just(testUser))
        whenever(userRepository.deleteById(userId)).thenReturn(Mono.empty())

        StepVerifier.create(userService.deleteProfile(userId))
            .verifyComplete()

        verify(userRepository).findById(userId)
        verify(userRepository).deleteById(userId)
    }

    @Test
    fun `deleteProfile should throw ResourceNotFoundException when user not found`() {
        val userId = 999L

        whenever(userRepository.findById(userId)).thenReturn(Mono.empty())

        StepVerifier.create(userService.deleteProfile(userId))
            .expectErrorMatches { error ->
                error is ResourceNotFoundException &&
                error.message == "Пользователь не найден"
            }
            .verify()

        verify(userRepository).findById(userId)
        verify(userRepository, never()).deleteById(any<Long>())
    }
}
