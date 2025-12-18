package ru.itmo.user.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import ru.itmo.user.exception.BadRequestException
import ru.itmo.user.exception.ConflictException
import ru.itmo.user.exception.ResourceNotFoundException
import ru.itmo.user.model.dto.response.UserResponse
import ru.itmo.user.model.entity.User
import ru.itmo.user.model.enums.UserRole
import ru.itmo.user.repository.UserRepository
import ru.itmo.user.repository.UserRoleRepository
import java.time.LocalDateTime

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userRoleRepository: UserRoleRepository
) {

    fun getUserById(userId: Long): Mono<UserResponse> {
        return userRepository.findById(userId)
            .flatMap { user ->
                userRoleRepository.findRolesByUserId(userId)
                    .map { UserRole.valueOf(it) }
                    .collect({ mutableSetOf<UserRole>() }) { set, role ->
                        set.add(role)
                    }
                    .map { roles -> user.copy(roles = roles) }
            }
            .map { it.toResponse() }
            .switchIfEmpty(Mono.error(ResourceNotFoundException("Пользователь $userId не найден")))
    }

    fun getCurrentUser(userId: Long): Mono<UserResponse> {
        return getUserById(userId)
    }

    @Transactional
    fun updateProfile(
        userId: Long,
        email: String?,
        firstName: String?,
        lastName: String?
    ): Mono<UserResponse> {
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(ResourceNotFoundException("Пользователь не найден")))
            .flatMap { user ->
                val newEmail = email ?: user.email

                if (email != null && email != user.email) {
                    if (!newEmail.matches(Regex("^[A-Za-z0-9+_.-]+@[^@]+\\.[A-Za-z]{2,}$"))) {
                        return@flatMap Mono.error(
                            BadRequestException("Некорректный формат email: $newEmail")
                        )
                    }

                    userRepository.existsByEmail(newEmail)
                        .flatMap { exists ->
                            if (exists) {
                                Mono.error(ConflictException("Email $newEmail уже используется"))
                            } else {
                                saveUpdatedUser(user, email, firstName, lastName)
                            }
                        }
                } else {
                    saveUpdatedUser(user, email, firstName, lastName)
                }
            }
    }

    private fun saveUpdatedUser(
        user: User,
        email: String?,
        firstName: String?,
        lastName: String?
    ): Mono<UserResponse> {
        val updatedUser = user.copy(
            email = email ?: user.email,
            firstName = firstName ?: user.firstName,
            lastName = lastName ?: user.lastName,
            updatedAt = LocalDateTime.now()
        )
        return userRepository.save(updatedUser)
            .flatMap { getUserById(updatedUser.id) }
    }

    @Transactional
    fun deleteProfile(userId: Long): Mono<Void> {
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(ResourceNotFoundException("Пользователь не найден")))
            .flatMap { userRepository.deleteById(userId) }
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
