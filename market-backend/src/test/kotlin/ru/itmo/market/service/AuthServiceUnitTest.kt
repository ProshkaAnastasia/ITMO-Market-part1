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
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import ru.itmo.market.exception.ConflictException
import ru.itmo.market.exception.UnauthorizedException
import ru.itmo.market.model.dto.request.LoginRequest
import ru.itmo.market.model.dto.request.RegisterRequest
import ru.itmo.market.model.entity.User
import ru.itmo.market.model.enums.UserRole
import ru.itmo.market.repository.UserRepository
import ru.itmo.market.security.jwt.JwtTokenProvider

@ExtendWith(MockitoExtension::class)
class AuthServiceUnitTest {

    @Mock
    private lateinit var userRepository: UserRepository
    
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
            userRepository,
            passwordEncoder,
            jwtTokenProvider,
            authenticationManager
        )
    }

    @Test
    fun `should register new user successfully`() {
        val request = RegisterRequest(
            username = "newuser",
            email = "newuser@example.com",
            password = "Password123",
            firstName = "John",
            lastName = "Doe"
        )

        whenever(userRepository.existsByUsername("newuser")).thenReturn(false)
        whenever(userRepository.existsByEmail("newuser@example.com")).thenReturn(false)
        
        val userCaptor = argumentCaptor<User>()
        whenever(userRepository.save(userCaptor.capture())).thenAnswer { invocation ->
            val user = invocation.arguments[0] as User
            user.copy(id = 1L)
        }
        
        whenever(passwordEncoder.encode("Password123")).thenReturn("hashed_password")
        
        whenever(jwtTokenProvider.generateAccessToken(eq(1L), eq("newuser"), eq(setOf(UserRole.USER.toString()))))
            .thenReturn("access_token")
        whenever(jwtTokenProvider.generateRefreshToken(eq(1L)))
            .thenReturn("refresh_token")

        val result = authService.register(request)

        assertNotNull(result, "AuthResponse не должен быть null")
        assertNotNull(result.accessToken, "accessToken не должен быть null")
        assertNotNull(result.refreshToken, "refreshToken не должен быть null")
        assertEquals("access_token", result.accessToken)
        assertEquals("refresh_token", result.refreshToken)
        
        verify(userRepository, times(1)).save(any())
        verify(jwtTokenProvider, times(1)).generateAccessToken(eq(1L), eq("newuser"), eq(setOf(UserRole.USER.toString())))
        verify(jwtTokenProvider, times(1)).generateRefreshToken(eq(1L))
    }


    @Test
    fun `should throw UnauthorizedException for invalid credentials`() {
        val request = LoginRequest(
            username = "testuser",
            password = "WrongPassword"
        )

        whenever(authenticationManager.authenticate(any()))
            .thenThrow(BadCredentialsException("Invalid credentials"))

        assertThrows<UnauthorizedException> {
            authService.login(request)
        }
    }
}
