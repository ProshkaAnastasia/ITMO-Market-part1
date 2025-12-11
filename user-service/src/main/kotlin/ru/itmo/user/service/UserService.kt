package ru.itmo.user.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.itmo.user.exception.BadRequestException
import ru.itmo.user.exception.ConflictException
import ru.itmo.user.exception.ResourceNotFoundException
import ru.itmo.user.model.dto.response.UserResponse
import ru.itmo.user.model.entity.User
import ru.itmo.user.repository.UserRepository


@Service
class UserService(
    private val userRepository: UserRepository
) {

    fun getUserById(userId: Long): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("Пользователь с ID $userId не найден") }
        return user.toResponse()
    }

    fun getCurrentUser(userId: Long): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("Пользователь не найден") }
        return user.toResponse()
    }

    @Transactional
    fun updateProfile(userId: Long, email: String?, firstName: String?, lastName: String?): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("Пользователь не найден") }

        email?.takeIf { it != user.email }?.let { newEmail ->
            if (!newEmail.matches(Regex("^[A-Za-z0-9+_.-]+@[^@]+\\.[A-Za-z]{2,}$"))) {
                throw BadRequestException("Некорректный формат email: $newEmail")
            }

            if (userRepository.existsByEmail(newEmail)) {
                throw ConflictException("Email $newEmail уже используется другим пользователем")
            }
        }

        val updatedUser = user.copy(
            email = email ?: user.email,
            firstName = firstName ?: user.firstName,
            lastName = lastName ?: user.lastName
        )

        val savedUser = userRepository.save(updatedUser)
        return savedUser.toResponse()
    }

    @Transactional
    fun deleteProfile(userId: Long) {
        userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("Пользователь не найден") }
        userRepository.deleteById(userId)
    }

    private fun User.toResponse(): UserResponse {
        return UserResponse(
            id = this.id,
            username = this.username,
            email = this.email,
            firstName = this.firstName,
            lastName = this.lastName,
            roles = this.roles.map { it.name }.toSet(),
            createdAt = this.createdAt
        )
    }
}