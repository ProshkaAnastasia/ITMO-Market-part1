package ru.itmo.user.service

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.itmo.user.exception.ConflictException
import ru.itmo.user.exception.ForbiddenException
import ru.itmo.user.exception.ResourceNotFoundException
import ru.itmo.user.model.dto.response.PaginatedResponse
import ru.itmo.user.model.dto.response.ProductResponse
import ru.itmo.user.model.dto.response.ShopResponse
import ru.itmo.user.model.entity.Shop
import ru.itmo.user.repository.ShopRepository
import ru.itmo.user.service.client.ProductServiceClient

@Service
class ShopService(
    private val shopRepository: ShopRepository,
    private val productServiceClient: ProductServiceClient,
    private val userService: UserService
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
        if (!shopRepository.existsById(shopId)) {
            throw ResourceNotFoundException("Магазин с ID $shopId не найден")
        }

        return productServiceClient.getProductsByShopId(shopId, page, pageSize)
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

    @Transactional
    fun deleteShop(shopId: Long, userId: Long) {
        val shop = shopRepository.findById(shopId)
            .orElseThrow { ResourceNotFoundException("Магазин с ID $shopId не найден") }

        val user = userService.getUserById(userId)

        if (shop.sellerId != userId && !user.roles.contains("ADMIN")) {
            throw ForbiddenException("У вас нет прав для удаления этого магазина")
        }

        shopRepository.deleteById(shopId)
    }

    private fun Shop.toResponse(): ShopResponse {
        val seller = userService.getUserById(this.sellerId)
        val productCount = productServiceClient.countProductsByShopId(this.id)

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