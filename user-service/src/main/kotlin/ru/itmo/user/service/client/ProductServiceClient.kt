package ru.itmo.user.service.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import ru.itmo.user.model.dto.response.PaginatedResponse
import ru.itmo.user.model.dto.response.ProductResponse
import ru.itmo.user.exception.ServiceUnavailableException


@FeignClient(
    name = "product-service",
    fallback = ProductServiceClientFallback::class
)
interface ProductServiceClient {

    @GetMapping("/api/products/shops/{shopId}")
    fun getProductsByShopId(
        @RequestParam("shopId") shopId: Long,
        @RequestParam("page") page: Int,
        @RequestParam("pageSize") pageSize: Int
    ): PaginatedResponse<ProductResponse>

    @GetMapping("/api/products/shops/{shopId}/count")
    fun countProductsByShopId(@RequestParam("shopId") shopId: Long): Long
}


@Component
class ProductServiceClientFallback : ProductServiceClient {
    override fun getProductsByShopId(
        shopId: Long,
        page: Int,
        pageSize: Int
    ): PaginatedResponse<ProductResponse> {
        throw ServiceUnavailableException("Product service is temporarily unavailable. Please try again later.")
    }

    override fun countProductsByShopId(shopId: Long): Long {
        throw ServiceUnavailableException("Product service is temporarily unavailable. Please try again later.")
    }
}