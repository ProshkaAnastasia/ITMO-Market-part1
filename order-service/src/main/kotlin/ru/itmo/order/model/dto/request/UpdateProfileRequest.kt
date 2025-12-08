package ru.itmo.user.model.dto.request

data class UpdateProfileRequest(
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null
)