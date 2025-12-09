package ru.itmo.order.model.dto.request

data class UpdateCommentRequest(
    val text: String? = null,
    val rating: Int? = null
)