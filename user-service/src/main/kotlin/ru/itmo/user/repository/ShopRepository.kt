package ru.itmo.user.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.itmo.user.model.entity.Shop

@Repository
interface ShopRepository : JpaRepository<Shop, Long> {
    fun existsBySellerId(sellerId: Long): Boolean
}