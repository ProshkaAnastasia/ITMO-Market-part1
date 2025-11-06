package ru.itmo.market.model.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

data class PaginatedResponse<T>(
    val data: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int
)