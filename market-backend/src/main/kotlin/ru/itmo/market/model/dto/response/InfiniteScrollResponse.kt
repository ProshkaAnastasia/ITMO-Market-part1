package ru.itmo.market.model.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

data class InfiniteScrollResponse<T>(
    val data: List<T>,
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean
)