package ru.itmo.market.service

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.itmo.market.exception.ConflictException
import ru.itmo.market.exception.ForbiddenException
import ru.itmo.market.exception.ResourceNotFoundException
import ru.itmo.market.model.dto.response.ShopResponse
import ru.itmo.market.model.dto.response.ProductResponse
import ru.itmo.market.model.dto.response.PaginatedResponse
import ru.itmo.market.model.entity.Shop
import ru.itmo.market.repository.ShopRepository
import ru.itmo.market.repository.ProductRepository
import ru.itmo.market.repository.UserRepository
import ru.itmo.market.repository.CommentRepository
import ru.itmo.market.model.enums.ProductStatus

@Service
class ShopService(
    private val shopRepository: ShopRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    private val commentRepository: CommentRepository
) {

    fun getAllShops(page: Int, pageSize: Int): PaginatedResponse<ShopResponse> {
        val pageable = PageRequest.of(page - 1, pageSize)
        val shopPage = shopRepository.findAll(pageable)
        return PaginatedResponse(
            data = shopPage.content.map { it.toResponse() },
            page = page,
            pageSize = pageSize,
            totalElements = shopPage.totalElements,
            totalPages = shopPage.totalPages
        )
    }

    fun getShopById(shopId: Long): ShopResponse {
        val shop = shopRepository.findById(shopId)
            .orElseThrow { ResourceNotFoundException("Магазин с ID $shopId не найден") }
        return shop.toResponse()
    }

    fun getShopProducts(shopId: Long, page: Int, pageSize: Int): PaginatedResponse<ProductResponse> {
        // Проверить что магазин существует
        shopRepository.findById(shopId)
            .orElseThrow { ResourceNotFoundException("Магазин с ID $shopId не найден") }

        val pageable = PageRequest.of(page - 1, pageSize)
        val productPage = productRepository.findAllByShopId(shopId, pageable)
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

    @Transactional
    fun createShop(sellerId: Long, name: String, description: String?, avatarUrl: String?): ShopResponse {
        // Проверить что продавец ещё не создал магазин
        if (shopRepository.existsBySellerId(sellerId)) {
            throw ConflictException("Вы уже создали магазин. Один продавец может иметь только один магазин")
        }

        if (name.isBlank()) {
            throw IllegalArgumentException("Название магазина не может быть пустым")
        }
        val shop = Shop(
            name = name,
            description = description,
            avatarUrl = avatarUrl,
            sellerId = sellerId
        )
        val savedShop = shopRepository.save(shop)
        return savedShop.toResponse()
    }

    @Transactional
    fun updateShop(
        shopId: Long,
        userId: Long,
        name: String?,
        description: String?,
        avatarUrl: String?
    ): ShopResponse {
        val shop = shopRepository.findById(shopId)
            .orElseThrow { ResourceNotFoundException("Магазин с ID $shopId не найден") }

        if (shop.sellerId != userId) {
            throw ForbiddenException("У вас нет прав для обновления этого магазина")
        }

        val updatedShop = shop.copy(
            name = name ?: shop.name,
            description = description ?: shop.description,
            avatarUrl = avatarUrl ?: shop.avatarUrl
        )

        val savedShop = shopRepository.save(updatedShop)
        return savedShop.toResponse()
    }

    private fun Shop.toResponse(): ShopResponse {
        val seller = userRepository.findById(this.sellerId)
            .orElseThrow { ResourceNotFoundException("Продавец не найден") }
        val productCount = productRepository.countByShopId(this.id) ?: 0L

        return ShopResponse(
            id = this.id,
            name = this.name,
            description = this.description,
            avatarUrl = this.avatarUrl,
            sellerId = this.sellerId,
            sellerName = seller.firstName + " " + seller.lastName,
            productsCount = productCount,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}

// Extension function для ProductRepository
fun ProductRepository.countByShopId(shopId: Long): Long? {
    val pageable = PageRequest.of(0, 1)
    return this.findAllByShopId(shopId, pageable).totalElements
}