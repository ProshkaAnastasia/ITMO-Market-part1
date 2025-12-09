package ru.itmo.product.model.dto.request

import jakarta.validation.constraints.*

data class AddToCartRequest(
    @field:NotNull(message = "productId не может быть null")
    val productId: Long,

    @field:Min(value = 1, message = "Количество должно быть минимум 1")
    val quantity: Int
)