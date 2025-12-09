package ru.itmo.order.model.dto.request

import jakarta.validation.constraints.*

data class RejectProductRequest(
    @field:NotBlank(message = "Причина отклонения не может быть пустой")
    val reason: String
)