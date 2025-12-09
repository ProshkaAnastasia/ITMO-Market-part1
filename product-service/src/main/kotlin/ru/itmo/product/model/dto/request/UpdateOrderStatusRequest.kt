package ru.itmo.product.model.dto.request

import jakarta.validation.constraints.*

data class UpdateOrderStatusRequest(
    @field:NotBlank(message = "Статус не может быть пустым")
    val status: String
)