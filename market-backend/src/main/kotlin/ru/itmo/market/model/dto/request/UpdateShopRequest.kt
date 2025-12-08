package ru.itmo.market.model.dto.request


data class UpdateShopRequest(
    val name: String? = null,
    val description: String? = null,
    val avatarUrl: String? = null
)