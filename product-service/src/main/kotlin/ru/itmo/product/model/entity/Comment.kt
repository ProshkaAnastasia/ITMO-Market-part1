package ru.itmo.product.model.entity

import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "comments")
data class Comment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @field:NotBlank(message = "Текст комментария не может быть пустым")
    @Column(columnDefinition = "TEXT", nullable = false)
    val text: String,

    @field:Min(value = 1, message = "Рейтинг должен быть от 1 до 5")
    @field:Max(value = 5, message = "Рейтинг должен быть от 1 до 5")
    @Column(nullable = false)
    val rating: Int,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
