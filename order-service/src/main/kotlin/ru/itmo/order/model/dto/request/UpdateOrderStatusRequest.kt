package ru.itmo.order.model.dto.request

import jakarta.validation.constraints.*

data class UpdateOrderStatusRequest(
    @field:NotBlank(message = "Статус не может быть пустым")
    val status: String
)