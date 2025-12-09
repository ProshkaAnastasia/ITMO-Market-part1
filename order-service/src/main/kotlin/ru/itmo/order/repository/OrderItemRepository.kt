package ru.itmo.order.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.itmo.order.model.entity.*
import java.util.Optional

@Repository
interface OrderItemRepository : JpaRepository<OrderItem, Long> {
    fun findByOrderIdAndProductId(orderId: Long, productId: Long): Optional<OrderItem>
    
    fun findAllByOrderId(orderId: Long): List<OrderItem>
    
    fun deleteByOrderId(orderId: Long)
}