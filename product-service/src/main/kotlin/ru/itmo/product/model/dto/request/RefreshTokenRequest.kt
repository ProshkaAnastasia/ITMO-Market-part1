package ru.itmo.user.model.dto.request

import jakarta.validation.constraints.*

data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh token не может быть пустым")
    val refreshToken: String
)