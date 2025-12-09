package ru.itmo.product.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.itmo.product.exception.ForbiddenException
import ru.itmo.product.model.dto.response.ProductResponse
import ru.itmo.product.model.dto.response.PaginatedResponse
import ru.itmo.product.service.client.UserServiceClient

@Service
class ModerationService(
    private val productService: ProductService,
    private val userServiceClient: UserServiceClient
) {

    @Transactional
    fun approveProduct(productId: Long, moderatorId: Long): ProductResponse {
        return productService.approveProduct(productId, moderatorId)
    }

    @Transactional
    fun rejectProduct(productId: Long, moderatorId: Long, reason: String): ProductResponse {
        return productService.rejectProduct(productId, moderatorId, reason)
    }

    fun getPendingProducts(moderatorId: Long, page: Int, pageSize: Int): PaginatedResponse<ProductResponse> {
        val moderator = userServiceClient.getUserById(moderatorId)
        if (!moderator.roles.contains("MODERATOR") && !moderator.roles.contains("ADMIN")) {
            throw ForbiddenException("Только модераторы и администраторы могут получать товары на модерации.")
        }
        return productService.getPendingProducts(page, pageSize)
    }

    fun getPendingProductById(moderatorId: Long, productId: Long): ProductResponse {
        val moderator = userServiceClient.getUserById(moderatorId)
        if (!moderator.roles.contains("MODERATOR") && !moderator.roles.contains("ADMIN")) {
            throw ForbiddenException("Только модераторы и администраторы могут получать товары на модерации.")
        }
        return productService.getPendingProductById(productId)
    }
}