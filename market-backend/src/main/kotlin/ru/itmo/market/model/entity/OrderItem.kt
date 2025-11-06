package ru.itmo.market.model.entity

import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import ru.itmo.market.model.enums.UserRole
import java.time.LocalDateTime

@Entity
@Table(
    name = "order_items",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["order_id", "product_id"])
    ]
)
data class OrderItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "order_id", nullable = false)
    val orderId: Long,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @field:Min(value = 1, message = "Количество должно быть больше 0")
    @Column(nullable = false)
    val quantity: Int,

    @Column(nullable = false)
    val price: java.math.BigDecimal,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)