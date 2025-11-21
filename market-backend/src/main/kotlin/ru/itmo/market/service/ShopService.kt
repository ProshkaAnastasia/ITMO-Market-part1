package ru.itmo.market.service

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.itmo.market.exception.ConflictException
import ru.itmo.market.exception.ForbiddenException
import ru.itmo.market.exception.ResourceNotFoundException
import ru.itmo.market.model.dto.response.PaginatedResponse
import ru.itmo.market.model.dto.response.ProductResponse
import ru.itmo.market.model.dto.response.ShopResponse
import ru.itmo.market.model.entity.Shop
import ru.itmo.market.repository.ShopRepository

@Service
class ShopService(
    private val shopRepository: ShopRepository,
    // We now inject Services, not Repositories
    private val productService: ProductService,
    private val userService: UserService
) {

    fun getAllShops(page: Int, pageSize: Int): PaginatedResponse<ShopResponse> {
        val pageable = PageRequest.of(page - 1, pageSize)
        val shopPage = shopRepository.findAll(pageable)
        
        return PaginatedResponse(
            data = shopPage.content.map { convertToResponse(it) },
            page = page,
            pageSize = pageSize,
            totalElements = shopPage.totalElements,
            totalPages = shopPage.totalPages
        )
    }

    fun getShopById(shopId: Long): ShopResponse {
        val shop = shopRepository.findById(shopId)
            .orElseThrow { ResourceNotFoundException("Магазин с ID $shopId не найден") }
        return convertToResponse(shop)
    }

    fun getShopProducts(shopId: Long, page: Int, pageSize: Int): PaginatedResponse<ProductResponse> {
        // 1. Verify Shop exists (ShopService's responsibility)
        if (!shopRepository.existsById(shopId)) {
            throw ResourceNotFoundException("Магазин с ID $shopId не найден")
        }

        // 2. Delegate data fetching to ProductService
        return productService.getProductsByShopId(shopId, page, pageSize)
    }

    @Transactional
    fun createShop(sellerId: Long, name: String, description: String?, avatarUrl: String?): ShopResponse {
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
        return convertToResponse(savedShop)
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
        return convertToResponse(savedShop)
    }

    /**
     * Helper method to convert Entity to Response.
     * This replaces the extension function so we can easily access injected services.
     */
    private fun convertToResponse(shop: Shop): ShopResponse {
        // Fetch data from other domains via Services
        val seller = userService.getUserById(shop.sellerId)
        val productCount = productService.countProductsByShopId(shop.id)

        return ShopResponse(
            id = shop.id,
            name = shop.name,
            description = shop.description,
            avatarUrl = shop.avatarUrl,
            sellerId = shop.sellerId,
            sellerName = seller.firstName + " " + seller.lastName,
            productsCount = productCount,
            createdAt = shop.createdAt,
            updatedAt = shop.updatedAt
        )
    }
}