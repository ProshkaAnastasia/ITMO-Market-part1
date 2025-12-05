package ru.itmo.market.service

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.context.annotation.Lazy
import ru.itmo.market.exception.ForbiddenException
import ru.itmo.market.exception.ResourceNotFoundException
import ru.itmo.market.model.dto.response.CommentResponse
import ru.itmo.market.model.dto.response.PaginatedResponse
import ru.itmo.market.model.entity.Comment
import ru.itmo.market.repository.CommentRepository

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    @Lazy private val productService: ProductService,
    private val userService: UserService
) {

    fun getProductComments(productId: Long, page: Int, pageSize: Int): PaginatedResponse<CommentResponse> {
        
        if (!productService.existsById(productId)) {
            throw ResourceNotFoundException("Товар с ID $productId не найден")
        }

        val pageable = PageRequest.of(page - 1, pageSize)
        val commentPage = commentRepository.findAllByProductId(productId, pageable)
        return PaginatedResponse(
            data = commentPage.content.map { comment ->
                
                val user = userService.getUserById(comment.userId)
                
                CommentResponse(
                    id = comment.id,
                    productId = comment.productId,
                    userId = comment.userId,
                    userName = user.username, 
                    text = comment.text,
                    rating = comment.rating,
                    createdAt = comment.createdAt,
                    updatedAt = comment.updatedAt
                )
            },
            page = page,
            pageSize = pageSize,
            totalElements = commentPage.totalElements,
            totalPages = commentPage.totalPages
        )
    }

    @Transactional
    fun createComment(productId: Long, userId: Long, text: String, rating: Int): CommentResponse {
        if (!productService.existsById(productId)) {
            throw ResourceNotFoundException("Товар с ID $productId не найден")
        }

        val comment = Comment(
            productId = productId,
            userId = userId,
            text = text,
            rating = rating
        )
        val savedComment = commentRepository.save(comment)
        
        val user = userService.getUserById(userId)

        return CommentResponse(
            id = savedComment.id,
            productId = savedComment.productId,
            userId = savedComment.userId,
            userName = user.username,
            text = savedComment.text,
            rating = savedComment.rating,
            createdAt = savedComment.createdAt,
            updatedAt = savedComment.updatedAt
        )
    }

    @Transactional
    fun updateComment(productId: Long, commentId: Long, userId: Long, text: String?, rating: Int?): CommentResponse {
        val comment = commentRepository.findByIdAndUserId(commentId, userId)
            .orElseThrow { ResourceNotFoundException("Комментарий не найден или у вас нет прав") }

        if (comment.productId != productId) {
            throw ForbiddenException("Этот комментарий не относится к данному товару")
        }

        val updatedComment = comment.copy(
            text = text ?: comment.text,
            rating = rating ?: comment.rating
        )

        val savedComment = commentRepository.save(updatedComment)
        
        val user = userService.getUserById(userId)

        return CommentResponse(
            id = savedComment.id,
            productId = savedComment.productId,
            userId = savedComment.userId,
            userName = user.username,
            text = savedComment.text,
            rating = savedComment.rating,
            createdAt = savedComment.createdAt,
            updatedAt = savedComment.updatedAt
        )
    }
    

    @Transactional
    fun deleteComment(productId: Long, commentId: Long, userId: Long) {
        val comment = commentRepository.findByIdAndUserId(commentId, userId)
            .orElseThrow { ResourceNotFoundException("Комментарий не найден или у вас нет прав") }

        if (comment.productId != productId) {
            throw ForbiddenException("Этот комментарий не относится к данному товару")
        }

        commentRepository.deleteById(commentId)
    }

    
    fun getAverageRatingByProductId(productId: Long): Double? {
        return commentRepository.getAverageRatingByProductId(productId)
    }

    fun getCommentCountByProductId(productId: Long): Long {
        return commentRepository.getCommentCountByProductId(productId)
    }
}