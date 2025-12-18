package ru.itmo.user.service

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import ru.itmo.user.exception.ConflictException
import ru.itmo.user.exception.ForbiddenException
import ru.itmo.user.exception.ResourceNotFoundException
import ru.itmo.user.exception.ServiceUnavailableException
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

    fun getAllShops(page: Int, pageSize: Int): Mono<PaginatedResponse<ShopResponse>> {
        return shopRepository.findAll()
            .skip(((page - 1) * pageSize).toLong())
            .take(pageSize.toLong())
            .collectList()
            .flatMap { shops ->
                shopRepository.count()
                    .flatMap { totalCount ->
                        // Собираем response для каждого магазина
                        shops.map { buildShopResponse(it) }
                            .let { monoList ->
                                Mono.zip(monoList) { responses ->
                                    responses.map { it as ShopResponse }.toList()
                                }
                            }
                            .map { responses ->
                                PaginatedResponse(
                                    data = responses,
                                    page = page,
                                    pageSize = pageSize,
                                    totalElements = totalCount,
                                    totalPages = ((totalCount + pageSize - 1) / pageSize).toInt()
                                )
                            }
                    }
            }
    }

    fun getShopById(shopId: Long): Mono<ShopResponse> {
        return shopRepository.findById(shopId)
            .switchIfEmpty(Mono.error(ResourceNotFoundException("Магазин с ID $shopId не найден")))
            .flatMap { shop -> buildShopResponse(shop) }
    }

    @CircuitBreaker(
        name = "productService",
        fallbackMethod = "getShopProductsFallback"
    )
    @TimeLimiter(
        name = "productService",
        fallbackMethod = "getShopProductsFallback"
    )
    fun getShopProducts(shopId: Long, page: Int, pageSize: Int): Mono<PaginatedResponse<ProductResponse>> {
        return shopRepository.existsById(shopId)
            .flatMap { exists ->
                if (!exists) {
                    Mono.error(ResourceNotFoundException("Магазин с ID $shopId не найден"))
                } else {
                    Mono.just(productServiceClient.getProductsByShopId(shopId, page, pageSize))
                }
            }
    }

    fun getShopProductsFallback(
        shopId: Long,
        page: Int,
        pageSize: Int,
        t: Throwable
    ): PaginatedResponse<ProductResponse> {
        throw ServiceUnavailableException("Product service is temporarily unavailable. Please try again later.")
    }

    @Transactional
    fun createShop(sellerId: Long, name: String, description: String?, avatarUrl: String?): Mono<ShopResponse> {
        if (name.isBlank()) {
            return Mono.error(IllegalArgumentException("Название магазина не может быть пустым"))
        }

        return shopRepository.existsBySellerId(sellerId)
            .flatMap { exists ->
                if (exists) {
                    Mono.error(ConflictException("Вы уже создали магазин. Один продавец может иметь только один магазин"))
                } else {
                    val shop = Shop(
                        name = name,
                        description = description,
                        avatarUrl = avatarUrl,
                        sellerId = sellerId
                    )
                    shopRepository.save(shop)
                        .flatMap { buildShopResponse(it) }
                }
            }
    }

    @Transactional
    fun updateShop(
        shopId: Long,
        userId: Long,
        name: String?,
        description: String?,
        avatarUrl: String?
    ): Mono<ShopResponse> {
        return shopRepository.findById(shopId)
            .switchIfEmpty(Mono.error(ResourceNotFoundException("Магазин с ID $shopId не найден")))
            .flatMap { shop ->
                if (shop.sellerId != userId) {
                    Mono.error(ForbiddenException("У вас нет прав для обновления этого магазина"))
                } else {
                    val updatedShop = shop.copy(
                        name = name ?: shop.name,
                        description = description ?: shop.description,
                        avatarUrl = avatarUrl ?: shop.avatarUrl
                    )
                    shopRepository.save(updatedShop)
                        .flatMap { buildShopResponse(it) }
                }
            }
    }

    @Transactional
    fun deleteShop(shopId: Long, userId: Long): Mono<Void> {
        return shopRepository.findById(shopId)
            .switchIfEmpty(Mono.error(ResourceNotFoundException("Магазин с ID $shopId не найден")))
            .flatMap { shop ->
                userService.getUserById(userId)
                    .flatMap { user ->
                        if (shop.sellerId != userId && !user.roles.contains("ADMIN")) {
                            Mono.error(ForbiddenException("У вас нет прав для удаления этого магазина"))
                        } else {
                            shopRepository.deleteById(shopId)
                        }
                    }
            }
    }

    @CircuitBreaker(
        name = "productService",
        fallbackMethod = "shopToResponseFallback"
    )
    @TimeLimiter(
        name = "productService",
        fallbackMethod = "shopToResponseFallback"
    )
    private fun buildShopResponse(shop: Shop): Mono<ShopResponse> {
        return Mono.zip(
            userService.getUserById(shop.sellerId),
            Mono.fromCallable { productServiceClient.countProductsByShopId(shop.id) }
                .subscribeOn(Schedulers.boundedElastic())
        ).map { tuple ->
            val seller = tuple.t1
            val productsCount = tuple.t2

            ShopResponse(
                id = shop.id,
                name = shop.name,
                description = shop.description,
                avatarUrl = shop.avatarUrl,
                sellerId = shop.sellerId,
                sellerName = "${seller.firstName} ${seller.lastName}",
                productsCount = productsCount,
                createdAt = shop.createdAt,
                updatedAt = shop.updatedAt
            )
        }
    }

    fun shopToResponseFallback(t: Throwable): ShopResponse {
        throw ServiceUnavailableException("Product service is temporarily unavailable. Please try again later.")
    }
}