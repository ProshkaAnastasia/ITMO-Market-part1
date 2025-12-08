package ru.itmo.product.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.itmo.market.exception.ForbiddenException
import ru.itmo.market.model.dto.response.ProductResponse
import ru.itmo.market.model.dto.response.PaginatedResponse

@Service
class ModerationService(
    private val productService: ProductService,
    private val userService: UserService
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
        val moderator = userService.getUserById(moderatorId)
        if (!moderator.roles.contains("MODERATOR") && !moderator.roles.contains("ADMIN")) {
            throw ForbiddenException("Только модераторы и администраторы могут получать товары на модерации.")
        }
        return productService.getPendingProducts(page, pageSize)
    }

    fun getPendingProductById(moderatorId: Long, productId: Long): ProductResponse {
        val moderator = userService.getUserById(moderatorId)
        if (!moderator.roles.contains("MODERATOR") && !moderator.roles.contains("ADMIN")) {
            throw ForbiddenException("Только модераторы и администраторы могут получать товары на модерации.")
        }
        return productService.getPendingProductById(productId)
    }
}