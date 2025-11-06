package ru.itmo.market.model.dto.request

import jakarta.validation.constraints.*
import java.math.BigDecimal

data class RejectProductRequest(
    @field:NotBlank(message = "Причина отклонения не может быть пустой")
    val reason: String
)