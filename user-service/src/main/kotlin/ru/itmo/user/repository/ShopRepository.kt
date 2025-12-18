package ru.itmo.user.repository

import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import ru.itmo.user.model.entity.Shop

@Repository
interface ShopRepository : R2dbcRepository<Shop, Long> {
    fun existsBySellerId(sellerId: Long): Mono<Boolean>
}
