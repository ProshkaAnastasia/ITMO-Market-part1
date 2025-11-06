package ru.itmo.market.model.dto.request

import jakarta.validation.constraints.*
import java.math.BigDecimal

data class UpdateOrderStatusRequest(
    @field:NotBlank(message = "Статус не может быть пустым")
    val status: String
)