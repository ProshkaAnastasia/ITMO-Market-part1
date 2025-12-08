package ru.itmo.user.model.dto.request

import jakarta.validation.constraints.*

data class LoginRequest(
    @field:NotBlank(message = "Username не может быть пустым")
    val username: String,

    @field:NotBlank(message = "Пароль не может быть пустым")
    val password: String
)