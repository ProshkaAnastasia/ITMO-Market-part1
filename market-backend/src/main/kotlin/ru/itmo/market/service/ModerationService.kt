package ru.itmo.market.service

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.itmo.market.exception.BadRequestException
import ru.itmo.market.exception.ResourceNotFoundException
import ru.itmo.market.model.dto.response.OrderResponse
import ru.itmo.market.model.dto.response.OrderItemResponse
import ru.itmo.market.model.dto.response.ProductResponse
import ru.itmo.market.model.dto.response.PaginatedResponse
import ru.itmo.market.model.entity.Order
import ru.itmo.market.model.entity.OrderItem
import ru.itmo.market.model.enums.OrderStatus
import ru.itmo.market.model.enums.ProductStatus
import ru.itmo.market.repository.*
import java.math.BigDecimal

@Service
class ModerationService(
    private val productRepository: ProductRepository,
    private val commentRepository: CommentRepository
) {

    /**
     * ТРАНЗАКЦИЯ №3: одобрить товар
     * Обоснование: все связанные операции (изменение статуса, логирование) должны быть выполнены вместе
     */
    @Transactional
    fun approvProduct(productId: Long, moderatorId: Long): ProductResponse {
        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Товар с ID $productId не найден") }

        if (product.status != ProductStatus.PENDING) {
            throw IllegalStateException("Может быть одобрен только товар со статусом PENDING")
        }

        val approvedProduct = product.copy(
            status = ProductStatus.APPROVED,
            rejectionReason = null
        )

        val savedProduct = productRepository.save(approvedProduct)
        return ProductResponse(
            id = savedProduct.id,
            name = savedProduct.name,
            description = savedProduct.description,
            price = savedProduct.price,
            imageUrl = savedProduct.imageUrl,
            shopId = savedProduct.shopId,
            sellerId = savedProduct.sellerId,
            status = savedProduct.status.name,
            rejectionReason = savedProduct.rejectionReason,
            createdAt = savedProduct.createdAt,
            updatedAt = savedProduct.updatedAt
        )
    }

    @Transactional
    fun rejectProduct(productId: Long, moderatorId: Long, reason: String): ProductResponse {
        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Товар с ID $productId не найден") }

        if (product.status != ProductStatus.PENDING) {
            throw IllegalStateException("Может быть отклонен только товар со статусом PENDING")
        }

        val rejectedProduct = product.copy(
            status = ProductStatus.REJECTED,
            rejectionReason = reason
        )

        val savedProduct = productRepository.save(rejectedProduct)
        return ProductResponse(
            id = savedProduct.id,
            name = savedProduct.name,
            description = savedProduct.description,
            price = savedProduct.price,
            imageUrl = savedProduct.imageUrl,
            shopId = savedProduct.shopId,
            sellerId = savedProduct.sellerId,
            status = savedProduct.status.name,
            rejectionReason = savedProduct.rejectionReason,
            createdAt = savedProduct.createdAt,
            updatedAt = savedProduct.updatedAt
        )
    }

    fun getPendingProducts(page: Int, pageSize: Int): PaginatedResponse<ProductResponse> {
        val pageable = PageRequest.of(page - 1, pageSize)
        val productPage = productRepository.findAllByStatus(ProductStatus.PENDING, pageable)
        return PaginatedResponse(
            data = productPage.content.map { product ->
                val avgRating = commentRepository.getAverageRatingByProductId(product.id)
                val commentCount = commentRepository.getCommentCountByProductId(product.id)
                
                ProductResponse(
                    id = product.id,
                    name = product.name,
                    description = product.description,
                    price = product.price,
                    imageUrl = product.imageUrl,
                    shopId = product.shopId,
                    sellerId = product.sellerId,
                    status = product.status.name,
                    rejectionReason = product.rejectionReason,
                    averageRating = avgRating,
                    commentsCount = commentCount,
                    createdAt = product.createdAt,
                    updatedAt = product.updatedAt
                )
            },
            page = page,
            pageSize = pageSize,
            totalElements = productPage.totalElements,
            totalPages = productPage.totalPages
        )
    }

    fun getPendingProductById(productId: Long): ProductResponse {
        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Товар с ID $productId не найден") }

        if (product.status != ProductStatus.PENDING) {
            throw ResourceNotFoundException("Товар не на модерации")
        }

        val avgRating = commentRepository.getAverageRatingByProductId(product.id)
        val commentCount = commentRepository.getCommentCountByProductId(product.id)

        return ProductResponse(
            id = product.id,
            name = product.name,
            description = product.description,
            price = product.price,
            imageUrl = product.imageUrl,
            shopId = product.shopId,
            sellerId = product.sellerId,
            status = product.status.name,
            rejectionReason = product.rejectionReason,
            averageRating = avgRating,
            commentsCount = commentCount,
            createdAt = product.createdAt,
            updatedAt = product.updatedAt
        )
    }
}

private data class ProductResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val price: java.math.BigDecimal,
    val imageUrl: String?,
    val shopId: Long,
    val sellerId: Long,
    val status: String,
    val rejectionReason: String?,
    val averageRating: Double? = null,
    val commentsCount: Long? = null,
    val createdAt: java.time.LocalDateTime,
    val updatedAt: java.time.LocalDateTime
)