package ru.itmo.order.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.itmo.order.model.entity.*
import ru.itmo.order.model.enums.OrderStatus
import java.util.Optional

@Repository
interface OrderRepository : JpaRepository<Order, Long> {
    fun findByUserIdAndStatus(userId: Long, status: OrderStatus): Optional<Order>
    
    fun findAllByUserIdAndStatusNot(userId: Long, status: OrderStatus, pageable: Pageable): Page<Order>
    
    fun findByIdAndUserId(orderId: Long, userId: Long): Optional<Order>
}