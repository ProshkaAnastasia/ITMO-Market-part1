package ru.itmo.market.user_domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.itmo.market.user_domain.model.entity.Shop

@Repository
interface ShopRepository : JpaRepository<Shop, Long> {
    fun existsBySellerId(sellerId: Long): Boolean
}