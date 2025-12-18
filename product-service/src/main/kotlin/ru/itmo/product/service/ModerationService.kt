package ru.itmo.product.service

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.itmo.product.exception.ForbiddenException
import ru.itmo.product.exception.ResourceNotFoundException
import ru.itmo.product.exception.ServiceUnavailableException
import ru.itmo.product.model.dto.response.PaginatedResponse
import ru.itmo.product.model.dto.response.ProductResponse
import ru.itmo.product.service.client.UserServiceClient


@Service
class ModerationService(
    private val productService: ProductService,
    private val userServiceClient: UserServiceClient
) {

    @CircuitBreaker(
        name = "userService",
        fallbackMethod = "getPendingProductsFallback"
    )
    @TimeLimiter(
        name = "userService",
        fallbackMethod = "getPendingProductsFallback"
    )
    fun getPendingProducts(moderatorId: Long, page: Int, pageSize: Int): PaginatedResponse<ProductResponse> {
        val moderator = userServiceClient.getUserById(moderatorId)
        if (!moderator.roles.contains("MODERATOR") && !moderator.roles.contains("ADMIN")) {
            throw ForbiddenException("Только модераторы и администраторы могут получать товары на модерации.")
        }
        return productService.getPendingProducts(page, pageSize)
    }

    fun getPendingProductsFallback(moderatorId: Long, page: Int, pageSize: Int, t: Throwable): PaginatedResponse<ProductResponse> {
        throw ServiceUnavailableException("User service is temporarily unavailable. Please try again later.")
    }

    @CircuitBreaker(
        name = "userService",
        fallbackMethod = "getPendingProductByIdFallback"
    )
    @TimeLimiter(
        name = "userService",
        fallbackMethod = "getPendingProductByIdFallback"
    )
    fun getPendingProductById(moderatorId: Long, productId: Long): ProductResponse {
        val moderator = userServiceClient.getUserById(moderatorId)
        if (!moderator.roles.contains("MODERATOR") && !moderator.roles.contains("ADMIN")) {
            throw ForbiddenException("Только модераторы и администраторы могут получать товары на модерации.")
        }
        return productService.getPendingProductById(productId)
    }

    fun getPendingProductByIdFallback(moderatorId: Long, productId: Long, t: Throwable): ProductResponse {
        throw ServiceUnavailableException("User service is temporarily unavailable. Please try again later.")
    }

    @Transactional
    @CircuitBreaker(
        name = "userService",
        fallbackMethod = "approveProductFallback"
    )
    @TimeLimiter(
        name = "userService",
        fallbackMethod = "approveProductFallback"
    )
    fun approveProduct(productId: Long, moderatorId: Long): ProductResponse {
        return productService.approveProduct(productId, moderatorId)
    }

    fun approveProductFallback(productId: Long, moderatorId: Long, t: Throwable): ProductResponse {
        throw ServiceUnavailableException("User service is temporarily unavailable. Please try again later.")
    }

    @Transactional
    @CircuitBreaker(
        name = "userService",
        fallbackMethod = "rejectProductFallback"
    )
    @TimeLimiter(
        name = "userService",
        fallbackMethod = "rejectProductFallback"
    )
    fun rejectProduct(productId: Long, moderatorId: Long, reason: String): ProductResponse {
        return productService.rejectProduct(productId, moderatorId, reason)
    }

    fun rejectProductFallback(productId: Long, moderatorId: Long, reason: String, t: Throwable): ProductResponse {
        throw ServiceUnavailableException("User service is temporarily unavailable. Please try again later.")
    }
}