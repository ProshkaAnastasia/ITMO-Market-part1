package ru.itmo.user.repository

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import ru.itmo.user.model.entity.UserRoleEntity

@Repository
interface UserRoleRepository : R2dbcRepository<UserRoleEntity, Long> {
    @Query("SELECT role FROM user_service.user_roles WHERE user_id = :userId")
    fun findRolesByUserId(userId: Long): Flux<String>
}