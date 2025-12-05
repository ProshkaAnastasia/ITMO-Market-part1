package ru.itmo.market.integration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.itmo.market.model.entity.User
import ru.itmo.market.model.enums.UserRole
import ru.itmo.market.repository.UserRepository
import java.time.LocalDateTime

@Component
class TestAuthHelper @Autowired constructor(

    private val userRepository: UserRepository
) {

    private var userCounter = 0L

    fun createTestUser(
        username: String = "testuser_${++userCounter}",
        email: String = "test_${userCounter}@example.com",
        password: String = "TestPassword123!",
        roles: Set<UserRole> = setOf(UserRole.USER)
    ): User {
        val user = User(

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

}
