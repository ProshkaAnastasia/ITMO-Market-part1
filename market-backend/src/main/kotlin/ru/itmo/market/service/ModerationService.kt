package ru.itmo.market.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.itmo.market.model.dto.response.ProductResponse
import ru.itmo.market.model.dto.response.PaginatedResponse

@Service
class ModerationService(
    private val productService: ProductService,
    private val commentService: CommentService
) {

    @Transactional
    fun approveProduct(productId: Long, moderatorId: Long): ProductResponse {
        return productService.approveProduct(productId, moderatorId)
    }

    @Transactional
    fun rejectProduct(productId: Long, moderatorId: Long, reason: String): ProductResponse {
        return productService.rejectProduct(productId, moderatorId, reason)
    }

    fun getPendingProducts(page: Int, pageSize: Int): PaginatedResponse<ProductResponse> {
        return productService.getPendingProducts(page, pageSize)
    }

    fun getPendingProductById(productId: Long): ProductResponse {
        return productService.getPendingProductById(productId)
    }
}