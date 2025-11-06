package ru.itmo.market.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import ru.itmo.market.model.entity.*
import ru.itmo.market.model.enums.OrderStatus
import ru.itmo.market.model.enums.ProductStatus
import java.util.Optional

@Repository
interface ShopRepository : JpaRepository<Shop, Long> {
    fun findBySellerId(sellerId: Long): Optional<Shop>
    fun existsBySellerId(sellerId: Long): Boolean
}