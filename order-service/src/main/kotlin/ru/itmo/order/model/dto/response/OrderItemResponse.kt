package ru.itmo.user.model.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

data class OrderItemResponse(
    val id: Long,
    val product: ProductResponse,
    val quantity: Int,
    val price: BigDecimal,
    val subtotal: BigDecimal,
    val createdAt: LocalDateTime
)