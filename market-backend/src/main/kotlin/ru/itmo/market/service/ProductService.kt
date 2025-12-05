package ru.itmo.market.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.itmo.market.exception.ForbiddenException
import ru.itmo.market.exception.ResourceNotFoundException
import ru.itmo.market.model.dto.request.UpdateProductRequest
import ru.itmo.market.model.dto.response.ProductResponse
import ru.itmo.market.model.dto.response.PaginatedResponse
import ru.itmo.market.model.dto.response.InfiniteScrollResponse
import ru.itmo.market.model.entity.Product
import ru.itmo.market.model.enums.ProductStatus
import ru.itmo.market.model.enums.UserRole
import ru.itmo.market.repository.CommentRepository
import ru.itmo.market.repository.ProductRepository
import org.springframework.data.domain.PageRequest
import ru.itmo.market.exception.UnauthorizedException
import ru.itmo.market.repository.ShopRepository

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val shopRepository: ShopRepository,
    private val commentRepository: CommentRepository
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

    fun createProduct(
        name: String,
        description: String?,
        price: java.math.BigDecimal,
        imageUrl: String?,
        shopId: Long,
        sellerId: Long
    ): ProductResponse {
        val shop = shopRepository.findById(shopId)
            .orElseThrow { ResourceNotFoundException("No such shop exists") }

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

    @Transactional
    fun updateProduct(
        productId: Long,
        userId: Long,
        userRoles: Set<String>,
        request: UpdateProductRequest
    ): ProductResponse {
        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Товар с ID $productId не найден") }

        // Проверка прав
        if (product.sellerId != userId && !userRoles.contains("MODERATOR")) {
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

    @Transactional
    fun deleteProduct(productId: Long, userId: Long, userRoles: Set<String>) {
        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Товар с ID $productId не найден") }

        if (product.sellerId != userId && !userRoles.contains("MODERATOR")) {
            throw ForbiddenException("У вас нет прав для удаления этого товара")
        }

        productRepository.deleteById(productId)
    }

    private fun Product.toResponse(): ProductResponse {
        val avgRating = commentRepository.getAverageRatingByProductId(this.id)
        val commentCount = commentRepository.getCommentCountByProductId(this.id)
        
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