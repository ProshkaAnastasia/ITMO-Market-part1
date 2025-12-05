package ru.itmo.market.service

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.itmo.market.exception.ForbiddenException
import ru.itmo.market.exception.ResourceNotFoundException
import ru.itmo.market.model.dto.response.UserResponse
import ru.itmo.market.model.dto.response.CommentResponse
import ru.itmo.market.model.dto.response.PaginatedResponse
import ru.itmo.market.model.entity.Comment
import ru.itmo.market.model.entity.Product
import ru.itmo.market.model.entity.User
import ru.itmo.market.model.enums.ProductStatus
import ru.itmo.market.repository.UserRepository
import ru.itmo.market.repository.CommentRepository
import ru.itmo.market.repository.ProductRepository

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

    fun findUserEntityByUsername(username: String): User {
        return userRepository.findByUsername(username)
            .orElseThrow { ResourceNotFoundException("Пользователь $username не найден") }
    }

    fun existsByUsername(username: String): Boolean {
        return userRepository.existsByUsername(username)
    }

    fun existsByEmail(email: String): Boolean {
        return userRepository.existsByEmail(email)
    }

    @Transactional
    fun saveUser(user: User): User {
        return userRepository.save(user)
    }

    fun findUserEntityById(id: Long): User {
        return userRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Пользователь с ID $id не найден") }
    }

}