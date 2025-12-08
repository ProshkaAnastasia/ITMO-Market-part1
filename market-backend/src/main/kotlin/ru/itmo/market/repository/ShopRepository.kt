package ru.itmo.market.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.itmo.market.model.entity.*

@Repository
interface ShopRepository : JpaRepository<Shop, Long> {
    fun existsBySellerId(sellerId: Long): Boolean
}