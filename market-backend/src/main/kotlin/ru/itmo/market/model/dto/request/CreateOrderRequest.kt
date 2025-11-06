package ru.itmo.market.model.dto.request

import jakarta.validation.constraints.*
import java.math.BigDecimal

data class CreateOrderRequest(
    @field:NotBlank(message = "Адрес доставки не может быть пустым")
    val deliveryAddress: String
)