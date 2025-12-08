package ru.itmo.user.model.dto.response

import java.time.LocalDateTime

data class CommentResponse(
    val id: Long,
    val productId: Long,
    val userId: Long,
    val userName: String? = null,
    val text: String,
    val rating: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)