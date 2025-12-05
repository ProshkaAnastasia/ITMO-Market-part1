package ru.itmo.market.integration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.itmo.market.model.entity.User
import ru.itmo.market.model.enums.UserRole
import ru.itmo.market.repository.UserRepository
import ru.itmo.market.security.jwt.JwtTokenProvider
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime

@Component
class TestAuthHelper {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    fun createTestUser(
        username: String = "testuser",
        email: String = "test@example.com",
        password: String = "TestPassword123!",
        roles: Set<UserRole> = setOf(UserRole.USER)
    ): User {
        val hashedPassword = passwordEncoder.encode(password)
        val user = User(
            id = 0L,
            username = username,
            email = email,
            password = hashedPassword,
            firstName = "Test",
            lastName = "User",
            roles = roles,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        return userRepository.save(user)
    }

    fun generateToken(userId: Long, username: String, roles: Set<String> = setOf("USER")): String {
        return jwtTokenProvider.generateAccessToken(userId, username, roles)
    }

    fun createTokenForUser(user: User): String {
        return generateToken(user.id, user.username, user.roles.map { it.name }.toSet())
    }
}
