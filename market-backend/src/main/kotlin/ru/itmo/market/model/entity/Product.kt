package ru.itmo.market.model.entity

import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "products")
data class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @field:NotBlank(message = "Название товара не может быть пустым")
    @Column(nullable = false)
    val name: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @field:DecimalMin(value = "0.01", message = "Цена должна быть больше 0")
    @Column(nullable = false)
    val price: java.math.BigDecimal,

    @Column(name = "image_url")
    val imageUrl: String? = null,

    @Column(name = "shop_id", nullable = false)
    val shopId: Long,

    @Column(name = "seller_id", nullable = false)
    val sellerId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: ru.itmo.market.model.enums.ProductStatus = ru.itmo.market.model.enums.ProductStatus.PENDING,

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    val rejectionReason: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)