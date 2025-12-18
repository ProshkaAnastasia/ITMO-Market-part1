package ru.itmo.user.repository

import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import ru.itmo.user.model.entity.User

@Repository
interface UserRepository : R2dbcRepository<User, Long> {
    fun existsByEmail(email: String): Mono<Boolean>
}