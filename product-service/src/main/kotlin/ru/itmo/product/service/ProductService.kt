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
import ru.itmo.product.model.dto.request.UpdateProductRequest
import ru.itmo.product.model.dto.response.ProductResponse
import ru.itmo.product.model.dto.response.PaginatedResponse
import ru.itmo.product.model.dto.response.InfiniteScrollResponse
import ru.itmo.product.model.entity.Product
import ru.itmo.product.model.enums.ProductStatus
import ru.itmo.product.repository.ProductRepository
import ru.itmo.product.service.client.ShopServiceClient
import ru.itmo.product.service.client.UserServiceClient
import java.math.BigDecimal


@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val shopServiceClient: ShopServiceClient,
    @Lazy private val commentService: CommentService,
    private val userServiceClient: UserServiceClient
) {

    fun getApprovedProducts(page: Int, pageSize: Int): PaginatedResponse<ProductResponse> {
        val pageable = PageRequest.of(page - 1, pageSize)
        val productPage = productRepository.findAllByStatus(ProductStatus.APPROVED, pageable)
        return PaginatedResponse(
            data = productPage.content.map { it.toResponse() },
            page = page,
            pageSize = pageSize,
            totalElements = productPage.totalElements,
            totalPages = productPage.totalPages
        )
    }

    fun getApprovedProductsInfinite(page: Int, pageSize: Int): InfiniteScrollResponse<ProductResponse> {
        val pageable = PageRequest.of(page - 1, pageSize)
        val productPage = productRepository.findAllByStatus(ProductStatus.APPROVED, pageable)
        return InfiniteScrollResponse(
            data = productPage.content.map { it.toResponse() },
            page = page,
            pageSize = pageSize,
            hasMore = !productPage.isLast
        )
    }

    fun searchProducts(keyword: String, page: Int, pageSize: Int): PaginatedResponse<ProductResponse> {
        val pageable = PageRequest.of(page - 1, pageSize)
        val productPage = productRepository.searchApprovedProducts(keyword, pageable)
        return PaginatedResponse(
            data = productPage.content.map { it.toResponse() },
            page = page,
            pageSize = pageSize,
            totalElements = productPage.totalElements,
            totalPages = productPage.totalPages
        )
    }

    fun getProductById(productId: Long): ProductResponse {
        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Товар с ID $productId не найден") }
        return product.toResponse()
    }

    fun existsById(productId: Long): Boolean {
        return productRepository.existsById(productId)
    }

    @Transactional
    @CircuitBreaker(
        name = "shopService",
        fallbackMethod = "createProductFallback"
    )
    @TimeLimiter(
        name = "shopService",
        fallbackMethod = "createProductFallback"
    )
    fun createProduct(
        name: String,
        description: String?,
        price: BigDecimal,
        imageUrl: String?,
        shopId: Long,
        sellerId: Long
    ): ProductResponse {
        val shop = shopServiceClient.getShopById(shopId)

        if (shop.sellerId != sellerId) {
            throw ForbiddenException("Only shop owner can add products")
        }

        val product = Product(
            name = name,
            description = description,
            price = price,
            imageUrl = imageUrl,
            shopId = shopId,
            sellerId = sellerId,
            status = ProductStatus.PENDING
        )
        val savedProduct = productRepository.save(product)
        return savedProduct.toResponse()
    }

    fun createProductFallback(
        name: String,
        description: String?,
        price: BigDecimal,
        imageUrl: String?,
        shopId: Long,
        sellerId: Long,
        t: Throwable
    ): ProductResponse {
        throw ServiceUnavailableException("Shop service is temporarily unavailable. Please try again later.")
    }

    @Transactional
    @CircuitBreaker(
        name = "userService",
        fallbackMethod = "updateProductFallback"
    )
    @TimeLimiter(
        name = "userService",
        fallbackMethod = "updateProductFallback"
    )
    fun updateProduct(
        productId: Long,
        userId: Long,
        request: UpdateProductRequest
    ): ProductResponse {
        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Товар с ID $productId не найден") }

        val user = userServiceClient.getUserById(userId)

        if (product.sellerId != userId && !user.roles.contains("MODERATOR")) {
            throw ForbiddenException("У вас нет прав для обновления этого товара")
        }

        val updatedProduct = product.copy(
            name = request.name ?: product.name,
            description = request.description ?: product.description,
            price = request.price ?: product.price,
            imageUrl = request.imageUrl ?: product.imageUrl
        )

        val savedProduct = productRepository.save(updatedProduct)
        return savedProduct.toResponse()
    }

    fun updateProductFallback(
        productId: Long,
        userId: Long,
        request: UpdateProductRequest,
        t: Throwable
    ): ProductResponse {
        throw ServiceUnavailableException("User service is temporarily unavailable. Please try again later.")
    }

    @Transactional
    @CircuitBreaker(
        name = "userService",
        fallbackMethod = "deleteProductFallback"
    )
    @TimeLimiter(
        name = "userService",
        fallbackMethod = "deleteProductFallback"
    )
    fun deleteProduct(productId: Long, userId: Long) {
        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Товар с ID $productId не найден") }

        val user = userServiceClient.getUserById(userId)

        if (product.sellerId != userId && !user.roles.contains("MODERATOR")) {
            throw ForbiddenException("У вас нет прав для удаления этого товара")
        }

        productRepository.deleteById(productId)
    }

    fun deleteProductFallback(productId: Long, userId: Long, t: Throwable) {
        throw ServiceUnavailableException("User service is temporarily unavailable. Please try again later.")
    }

    fun countProductsByShopId(shopId: Long): Long {
        return productRepository.countByShopId(shopId)
    }

    fun getProductsByShopId(shopId: Long, page: Int, pageSize: Int): PaginatedResponse<ProductResponse> {
        val pageable = PageRequest.of(page - 1, pageSize)
        val productPage = productRepository.findAllByShopId(shopId, pageable)
        
        return PaginatedResponse(
            data = productPage.content.map { it.toResponse() },
            page = page,
            pageSize = pageSize,
            totalElements = productPage.totalElements,
            totalPages = productPage.totalPages
        )
    }

    @Transactional
    @CircuitBreaker(
        name = "userService",
        fallbackMethod = "approveFallback"
    )
    @TimeLimiter(
        name = "userService",
        fallbackMethod = "approveFallback"
    )
    fun approveProduct(productId: Long, moderatorId: Long): ProductResponse {
        val moderator = userServiceClient.getUserById(moderatorId)
        if (!moderator.roles.contains("MODERATOR") && !moderator.roles.contains("ADMIN")) {
            throw ForbiddenException("Только модераторы и администраторы могут одобрять товары")
        }

        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Товар с ID $productId не найден") }

        if (product.status != ProductStatus.PENDING) {
            throw IllegalStateException("Может быть одобрен только товар со статусом PENDING")
        }

        val approvedProduct = product.copy(status = ProductStatus.APPROVED, rejectionReason = null)
        val savedProduct = productRepository.save(approvedProduct)
        return savedProduct.toResponse()
    }

    fun approveFallback(productId: Long, moderatorId: Long, t: Throwable): ProductResponse {
        throw ServiceUnavailableException("User service is temporarily unavailable. Please try again later.")
    }

    @Transactional
    @CircuitBreaker(
        name = "userService",
        fallbackMethod = "rejectFallback"
    )
    @TimeLimiter(
        name = "userService",
        fallbackMethod = "rejectFallback"
    )
    fun rejectProduct(productId: Long, moderatorId: Long, reason: String): ProductResponse {
        val moderator = userServiceClient.getUserById(moderatorId)
        if (!moderator.roles.contains("MODERATOR") && !moderator.roles.contains("ADMIN")) {
            throw ForbiddenException("Только модераторы и администраторы могут отклонять товары")
        }

        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Товар с ID $productId не найден") }

        if (product.status != ProductStatus.PENDING) {
            throw IllegalStateException("Может быть отклонен только товар со статусом PENDING")
        }

        val rejectedProduct = product.copy(status = ProductStatus.REJECTED, rejectionReason = reason)
        val savedProduct = productRepository.save(rejectedProduct)
        return savedProduct.toResponse()
    }

    fun rejectFallback(productId: Long, moderatorId: Long, reason: String, t: Throwable): ProductResponse {
        throw ServiceUnavailableException("User service is temporarily unavailable. Please try again later.")
    }

    fun getPendingProducts(page: Int, pageSize: Int): PaginatedResponse<ProductResponse> {
        val pageable = PageRequest.of(page - 1, pageSize)
        val productPage = productRepository.findAllByStatus(ProductStatus.PENDING, pageable)
        return PaginatedResponse(
            data = productPage.content.map { it.toResponse() },
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

        return product.toResponse()
    }

    private fun Product.toResponse(): ProductResponse {
        val avgRating = commentService.getAverageRatingByProductId(this.id)
        val commentCount = commentService.getCommentCountByProductId(this.id)
        
        return ProductResponse(
            id = this.id,
            name = this.name,
            description = this.description,
            price = this.price,
            imageUrl = this.imageUrl,
            shopId = this.shopId,
            sellerId = this.sellerId,
            status = this.status.name,
            rejectionReason = this.rejectionReason,
            averageRating = avgRating,
            commentsCount = commentCount,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}