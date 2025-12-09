package ru.itmo.order.model.dto.request

import jakarta.validation.constraints.*
import java.math.BigDecimal

data class CreateProductRequest(
    @field:NotBlank(message = "Название товара не может быть пустым")
    val name: String,

    val description: String? = null,

    @field:DecimalMin(value = "0.01", message = "Цена должна быть больше 0")
    val price: BigDecimal,

    val imageUrl: String? = null,

    @field:NotNull(message = "shopId не может быть null")
    val shopId: Long
)