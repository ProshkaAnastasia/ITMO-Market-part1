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
interface ProductRepository : JpaRepository<Product, Long> {
    fun findAllByStatus(status: ProductStatus, pageable: Pageable): Page<Product>
    
    fun findAllByShopId(shopId: Long, pageable: Pageable): Page<Product>
    
    @Query(
        """
        SELECT p FROM Product p 
        WHERE p.status = 'APPROVED' 
        AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
             OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY p.createdAt DESC
        """
    )
    fun searchApprovedProducts(keyword: String, pageable: Pageable): Page<Product>
    
    fun findAllByStatusAndShopId(status: ProductStatus, shopId: Long, pageable: Pageable): Page<Product>
}