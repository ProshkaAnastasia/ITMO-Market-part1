package ru.itmo.user.model.entity

import jakarta.validation.constraints.*
import ru.itmo.user.model.enums.UserRole
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table(name = "users", schema = "user_service")
data class User(
    @Id
    val id: Long = 0,

    @field:NotBlank(message = "Username не может быть пустым")
    @field:Size(min = 4, max = 32, message = "Username от 4 до 32 символов")
    @Column("username")
    val username: String,

    @field:NotBlank(message = "Email не может быть пустым")
    @field:Email(message = "Некорректный формат email")
    @Column("email")
    val email: String,

    @field:NotBlank(message = "Пароль не может быть пустым")
    @Column("password")
    val password: String,

    @field:NotBlank(message = "Имя не может быть пустым")
    @Column("first_name")
    val firstName: String,

    @field:NotBlank(message = "Фамилия не может быть пустой")
    @Column("last_name")
    val lastName: String,

    @Column("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column("updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Transient
    val roles: Set<UserRole> = emptySet()
)