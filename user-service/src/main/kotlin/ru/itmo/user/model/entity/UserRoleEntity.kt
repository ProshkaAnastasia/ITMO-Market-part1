package ru.itmo.user.model.entity

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("user_roles")
data class UserRoleEntity(
    @Column("user_id")
    val userId: Long,

    @Column("role")
    val role: String
)
