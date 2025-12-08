package ru.itmo.user.model.dto.response

import java.time.LocalDateTime

data class ErrorResponse(
    val message: String?,
    val errors: List<String>? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val path: String? = null,
    val status: Int? = null
)