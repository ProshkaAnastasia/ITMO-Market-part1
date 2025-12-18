package ru.itmo.product.service

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.context.annotation.Lazy
import ru.itmo.product.exception.ForbiddenException
import ru.itmo.product.exception.ResourceNotFoundException
import ru.itmo.product.exception.ServiceUnavailableException
import ru.itmo.product.model.dto.response.CommentResponse
import ru.itmo.product.model.dto.response.PaginatedResponse
import ru.itmo.product.model.entity.Comment
import ru.itmo.product.repository.CommentRepository
import ru.itmo.product.service.client.UserServiceClient


@Service
class CommentService(
    private val commentRepository: CommentRepository,
    @Lazy private val productService: ProductService,
    private val userServiceClient: UserServiceClient
) {

    fun getProductComments(productId: Long, page: Int, pageSize: Int): PaginatedResponse<CommentResponse> {
        if (!productService.existsById(productId)) {
            throw ResourceNotFoundException("Товар с ID $productId не найден")
        }

        val pageable = PageRequest.of(page - 1, pageSize)
        val commentPage = commentRepository.findAllByProductId(productId, pageable)
        return PaginatedResponse(
            data = commentPage.content.map { comment ->
                val user = userServiceClient.getUserById(comment.userId)
                
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
    @CircuitBreaker(
        name = "userService",
        fallbackMethod = "createCommentFallback"
    )
    @TimeLimiter(
        name = "userService",
        fallbackMethod = "createCommentFallback"
    )
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
        
        val user = userServiceClient.getUserById(userId)

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

    fun createCommentFallback(
        productId: Long,
        userId: Long,
        text: String,
        rating: Int,
        t: Throwable
    ): CommentResponse {
        throw ServiceUnavailableException("User service is temporarily unavailable. Please try again later.")
    }

    @Transactional
    @CircuitBreaker(
        name = "userService",
        fallbackMethod = "updateCommentFallback"
    )
    @TimeLimiter(
        name = "userService",
        fallbackMethod = "updateCommentFallback"
    )
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
        
        val user = userServiceClient.getUserById(userId)

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

    fun updateCommentFallback(
        productId: Long,
        commentId: Long,
        userId: Long,
        text: String?,
        rating: Int?,
        t: Throwable
    ): CommentResponse {
        throw ServiceUnavailableException("User service is temporarily unavailable. Please try again later.")
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