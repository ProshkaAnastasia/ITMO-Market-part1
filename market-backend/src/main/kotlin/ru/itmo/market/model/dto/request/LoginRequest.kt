package ru.itmo.market.model.dto.request

import jakarta.validation.constraints.*
import java.math.BigDecimal

data class LoginRequest(
    @field:NotBlank(message = "Username не может быть пустым")
    val username: String,

    @field:NotBlank(message = "Пароль не может быть пустым")
    val password: String
)