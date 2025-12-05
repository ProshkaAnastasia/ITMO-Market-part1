package ru.itmo.market.integration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.itmo.market.model.entity.User
import ru.itmo.market.model.enums.UserRole
import ru.itmo.market.repository.UserRepository
import java.time.LocalDateTime

@Component
class TestAuthHelper @Autowired constructor(
    // ✅ Предпочтительное внедрение через конструктор (не нужно lateinit var)
    private val userRepository: UserRepository
) {
    // ✅ Добавляем счетчик для обеспечения уникальности данных пользователя в каждом тесте
    private var userCounter = 0L

    fun createTestUser(
        username: String = "testuser_${++userCounter}", // ✅ Уникальное имя
        email: String = "test_${userCounter}@example.com", // ✅ Уникальный email
        password: String = "TestPassword123!",
        roles: Set<UserRole> = setOf(UserRole.USER)
    ): User {
        val user = User(
            // ❌ УДАЛЕН id = 0L. Spring/JPA сам присвоит ID, не нужно его устанавливать.
            username = username,
            email = email,
            password = password,
            firstName = "Test",
            lastName = "User",
            roles = roles,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        return userRepository.save(user)
    }


    // fun generateToken(userId: Long, username: String, roles: Set<String> = setOf("USER")): String {
    //     return jwtTokenProvider.generateAccessToken(userId, username, roles)
    // }

    // fun createTokenForUser(user: User): String {
    //     return generateToken(user.id, user.username, user.roles.map { it.name }.toSet())
    // }
}
