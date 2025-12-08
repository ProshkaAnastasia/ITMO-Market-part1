package ru.itmo.user.model.dto.request

import jakarta.validation.constraints.*

data class CreateCommentRequest(
    @field:NotBlank(message = "Текст комментария не может быть пустым")
    val text: String,

    @field:Min(value = 1, message = "Рейтинг должен быть от 1 до 5")
    @field:Max(value = 5, message = "Рейтинг должен быть от 1 до 5")
    val rating: Int
)