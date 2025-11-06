package ru.itmo.market.model.dto.request

import jakarta.validation.constraints.*
import java.math.BigDecimal

data class CreateShopRequest(
    @field:NotBlank(message = "Название магазина не может быть пустым")
    val name: String,

    val description: String? = null,
    val avatarUrl: String? = null
)