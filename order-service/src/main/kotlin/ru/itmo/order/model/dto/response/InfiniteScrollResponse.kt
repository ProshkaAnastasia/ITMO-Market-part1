package ru.itmo.order.model.dto.response

data class InfiniteScrollResponse<T>(
    val data: List<T>,
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean
)