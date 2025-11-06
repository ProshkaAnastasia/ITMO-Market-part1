package ru.itmo.market.model.dto.request

import jakarta.validation.constraints.*
import java.math.BigDecimal

data class UpdateShopRequest(
    val name: String? = null,
    val description: String? = null,
    val avatarUrl: String? = null
)