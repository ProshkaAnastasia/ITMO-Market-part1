package ru.itmo.market.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import ru.itmo.market.model.entity.*
import java.util.Optional

@Repository
interface CommentRepository : JpaRepository<Comment, Long> {
    fun findAllByProductId(productId: Long, pageable: Pageable): Page<Comment>
    
    @Query("SELECT AVG(c.rating) FROM Comment c WHERE c.productId = :productId")
    fun getAverageRatingByProductId(productId: Long): Double?
    
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.productId = :productId")
    fun getCommentCountByProductId(productId: Long): Long
    
    fun findByIdAndUserId(commentId: Long, userId: Long): Optional<Comment>
}