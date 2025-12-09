package ru.itmo.order.model.dto.request

data class UpdateProfileRequest(
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null
)