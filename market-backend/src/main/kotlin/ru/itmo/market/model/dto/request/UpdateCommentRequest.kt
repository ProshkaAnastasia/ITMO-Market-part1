package ru.itmo.market.model.dto.request

import jakarta.validation.constraints.*
import java.math.BigDecimal

data class UpdateCommentRequest(
    val text: String? = null,
    val rating: Int? = null
)