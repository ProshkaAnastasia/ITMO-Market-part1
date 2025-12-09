package ru.itmo.order.model.dto.request

import jakarta.validation.constraints.*

data class CreateOrderRequest(
    @field:NotBlank(message = "Адрес доставки не может быть пустым")
    val deliveryAddress: String
)