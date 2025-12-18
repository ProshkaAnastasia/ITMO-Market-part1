package ru.itmo.user.integration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.itmo.user.model.entity.User
import ru.itmo.user.model.entity.UserRoleEntity
import ru.itmo.user.model.enums.UserRole
import ru.itmo.user.repository.UserRepository
import ru.itmo.user.repository.UserRoleRepository
import java.time.LocalDateTime

@Component
class TestAuthHelper @Autowired constructor(
    private val userRepository: UserRepository,
    private val userRoleRepository: UserRoleRepository
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
            roles = emptySet(),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val savedUser = userRepository.save(user).block()!!

        // Save user roles to the user_roles table
        roles.forEach { role ->
            userRoleRepository.save(
                UserRoleEntity(
                    userId = savedUser.id,
                    role = role.name
                )
            ).block()
        }

        return savedUser.copy(roles = roles)
    }

}
