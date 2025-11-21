package ru.itmo.market.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.crypto.password.PasswordEncoder
import ru.itmo.market.exception.ConflictException
import ru.itmo.market.model.dto.request.RegisterRequest
import ru.itmo.market.model.entity.User
import ru.itmo.market.model.enums.UserRole
import ru.itmo.market.security.jwt.JwtTokenProvider
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class AuthServiceUnitTest {

    @Mock
    private lateinit var userService: UserService // ✅ FIXED: Service, not Repo

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Mock
    private lateinit var authenticationManager: AuthenticationManager

    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        authService = AuthService(
            userService, // ✅ FIXED
            passwordEncoder,
            jwtTokenProvider,
            authenticationManager
        )
    }

    @Test
    fun `should register user successfully`() {
        val request = RegisterRequest(
            username = "testuser",
            email = "test@example.com",
            password = "password",
            firstName = "John",
            lastName = "Doe"
        )

        val savedUser = User(
            id = 1L,
            username = request.username,
            email = request.email,
            password = "encoded_password",
            firstName = request.firstName,
            lastName = request.lastName,
            roles = setOf(UserRole.USER),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        // Mock checks
        whenever(userService.existsByUsername(request.username)).thenReturn(false)
        whenever(userService.existsByEmail(request.email)).thenReturn(false)
        
        // Mock encoder
        whenever(passwordEncoder.encode(request.password)).thenReturn("encoded_password")

        // Mock save
        whenever(userService.saveUser(any())).thenReturn(savedUser)

        // Mock tokens
        whenever(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("access_token")
        whenever(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh_token")

        val result = authService.register(request)

        assertNotNull(result)
        assertEquals("access_token", result.accessToken)
        verify(userService).saveUser(any())
    }

    @Test
    fun `should throw ConflictException when username exists`() {
        val request = RegisterRequest("user", "email", "pass", "fn", "ln")
        whenever(userService.existsByUsername(request.username)).thenReturn(true)

        assertThrows<ConflictException> {
            authService.register(request)
        }
    }
}