package ru.itmo.product.model.dto.request

import jakarta.validation.constraints.*

data class CreateShopRequest(
    @field:NotBlank(message = "Название магазина не может быть пустым")
    val name: String,

    val description: String? = null,
    val avatarUrl: String? = null
)