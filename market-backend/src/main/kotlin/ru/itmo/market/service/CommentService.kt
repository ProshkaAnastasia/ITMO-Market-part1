package ru.itmo.market.service

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.itmo.market.exception.ForbiddenException
import ru.itmo.market.exception.ResourceNotFoundException
import ru.itmo.market.model.dto.response.UserResponse
import ru.itmo.market.model.dto.response.CommentResponse
import ru.itmo.market.model.dto.response.PaginatedResponse
import ru.itmo.market.model.entity.Comment
import ru.itmo.market.model.entity.Product
import ru.itmo.market.model.enums.ProductStatus
import ru.itmo.market.repository.UserRepository
import ru.itmo.market.repository.CommentRepository
import ru.itmo.market.repository.ProductRepository

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository
) {

    fun getProductComments(productId: Long, page: Int, pageSize: Int): PaginatedResponse<CommentResponse> {
        // Проверить что товар существует
        productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Товар с ID $productId не найден") }

        val pageable = PageRequest.of(page - 1, pageSize)
        val commentPage = commentRepository.findAllByProductId(productId, pageable)
        return PaginatedResponse(
            data = commentPage.content.map { comment ->
                val user = userRepository.findById(comment.userId)
                    .orElseThrow { ResourceNotFoundException("Пользователь не найден") }
                CommentResponse(
                    id = comment.id,
                    productId = comment.productId,
                    userId = comment.userId,
                    userName = user.firstName + " " + user.lastName,
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
        // Проверить что товар существует
        productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Товар с ID $productId не найден") }

        val comment = Comment(
            productId = productId,
            userId = userId,
            text = text,
            rating = rating
        )
        val savedComment = commentRepository.save(comment)
        
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("Пользователь не найден") }

        return CommentResponse(
            id = savedComment.id,
            productId = savedComment.productId,
            userId = savedComment.userId,
            userName = user.firstName + " " + user.lastName,
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
        
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("Пользователь не найден") }

        return CommentResponse(
            id = savedComment.id,
            productId = savedComment.productId,
            userId = savedComment.userId,
            userName = user.firstName + " " + user.lastName,
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
}