package ru.itmo.user.model.entity

import jakarta.validation.constraints.*
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table(name = "shops", schema = "user_service")
data class Shop(
    @Id
    val id: Long = 0,

    @field:NotBlank(message = "Название магазина не может быть пустым")
    @field:Size(max = 200)
    @Column("name")
    val name: String,

    @Column("description")
    val description: String? = null,

    @Column("avatar_url")
    val avatarUrl: String? = null,

    @Column("seller_id")
    val sellerId: Long,

    @Column("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column("updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
