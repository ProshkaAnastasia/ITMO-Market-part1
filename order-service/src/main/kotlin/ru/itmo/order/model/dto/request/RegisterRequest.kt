package ru.itmo.order.model.dto.request

import jakarta.validation.constraints.*

data class RegisterRequest(
    @field:NotBlank(message = "Username не может быть пустым")
    @field:Size(min = 4, max = 32, message = "Username от 4 до 32 символов")
    val username: String,

    @field:NotBlank(message = "Email не может быть пустым")
    @field:Email(message = "Некорректный формат email")
    val email: String,

    @field:NotBlank(message = "Пароль не может быть пустым")
    @field:Size(min = 8, max = 72, message = "Пароль от 8 до 72 символов")
    val password: String,

    @field:NotBlank(message = "Имя не может быть пустым")
    val firstName: String,

    @field:NotBlank(message = "Фамилия не может быть пустой")
    val lastName: String
)