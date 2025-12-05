package ru.itmo.market.service

import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.itmo.market.exception.ConflictException
import ru.itmo.market.exception.UnauthorizedException
import ru.itmo.market.model.dto.request.LoginRequest
import ru.itmo.market.model.dto.request.RegisterRequest
import ru.itmo.market.model.dto.response.TokenResponse
import ru.itmo.market.model.entity.User
import ru.itmo.market.model.enums.UserRole
import ru.itmo.market.repository.UserRepository
import ru.itmo.market.security.jwt.JwtTokenProvider
import java.time.LocalDateTime

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val authenticationManager: AuthenticationManager
) {

    @Transactional
    fun register(request: RegisterRequest): TokenResponse {
        // Проверка на существование пользователя
        if (userRepository.existsByUsername(request.username)) {
            throw ConflictException("Username '${request.username}' уже занят")
        }
        if (userRepository.existsByEmail(request.email)) {
            throw ConflictException("Email '${request.email}' уже зарегистрирован")
        }

        // Создание нового пользователя
        val user = User(
            username = request.username,
            email = request.email,
            password = passwordEncoder.encode(request.password),
            firstName = request.firstName,
            lastName = request.lastName,
            roles = setOf(UserRole.USER),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val savedUser = userRepository.save(user)

        // Генерация токенов
        val accessToken = jwtTokenProvider.generateAccessToken(
            savedUser.id,
            savedUser.username,
            savedUser.roles.map { it.name }.toSet()
        )
        val refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.id)

        return TokenResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = 900 // 15 minutes
        )
    }

    fun login(request: LoginRequest): TokenResponse {
        try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(
                    request.username,
                    request.password
                )
            )
        } catch (e: BadCredentialsException) {
            throw UnauthorizedException("Неверное имя пользователя или пароль")
        }

        val user = userRepository.findByUsername(request.username)
            .orElseThrow { UnauthorizedException("Пользователь не найден") }

        // Генерация токенов
        val accessToken = jwtTokenProvider.generateAccessToken(
            user.id,
            user.username,
            user.roles.map { it.name }.toSet()
        )
        val refreshToken = jwtTokenProvider.generateRefreshToken(user.id)

        return TokenResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = 900 // 15 minutes
        )
    }

    fun refresh(refreshToken: String): TokenResponse {
        if (!jwtTokenProvider.isTokenValid(refreshToken)) {
            throw UnauthorizedException("Невалидный refresh token")
        }

        val userId = jwtTokenProvider.getUserIdFromToken(refreshToken)
        val user = userRepository.findById(userId)
            .orElseThrow { UnauthorizedException("Пользователь не найден") }

        // Генерация новых токенов
        val newAccessToken = jwtTokenProvider.generateAccessToken(
            user.id,
            user.username,
            user.roles.map { it.name }.toSet()
        )
        val newRefreshToken = jwtTokenProvider.generateRefreshToken(user.id)

        return TokenResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            expiresIn = 900 // 15 minutes
        )
    }
}