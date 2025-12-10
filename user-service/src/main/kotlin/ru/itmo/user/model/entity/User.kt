package ru.itmo.user.model.entity

import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import ru.itmo.user.model.enums.UserRole
import java.time.LocalDateTime

@Entity
@Table(name = "users", schema = "user_service", uniqueConstraints = [
    UniqueConstraint(columnNames = ["username"]),
    UniqueConstraint(columnNames = ["email"])
])
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @field:NotBlank(message = "Username не может быть пустым")
    @field:Size(min = 4, max = 32, message = "Username от 4 до 32 символов")
    @Column(unique = true, nullable = false)
    val username: String,

    @field:NotBlank(message = "Email не может быть пустым")
    @field:Email(message = "Некорректный формат email")
    @Column(unique = true, nullable = false)
    val email: String,

    @field:NotBlank(message = "Пароль не может быть пустым")
    @Column(nullable = false)
    val password: String,

    @field:NotBlank(message = "Имя не может быть пустым")
    @Column(name = "first_name", nullable = false)
    val firstName: String,

    @field:NotBlank(message = "Фамилия не может быть пустой")
    @Column(name = "last_name", nullable = false)
    val lastName: String,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", schema = "user_service", joinColumns = [JoinColumn(name = "user_id")])
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    val roles: Set<UserRole> = setOf(UserRole.USER),

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)