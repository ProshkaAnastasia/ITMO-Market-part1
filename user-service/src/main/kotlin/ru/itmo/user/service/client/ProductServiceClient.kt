package ru.itmo.user.service.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import ru.itmo.user.model.dto.response.PaginatedResponse
import ru.itmo.user.model.dto.response.ProductResponse

@FeignClient(name = "product-service")
interface ProductServiceClient {

    @GetMapping("/api/products")
    fun getProductsByShopId(
        @RequestParam("shopId") shopId: Long,
        @RequestParam("page") page: Int,
        @RequestParam("pageSize") pageSize: Int
    ): PaginatedResponse<ProductResponse>

    @GetMapping("/api/products/count")
    fun countProductsByShopId(@RequestParam("shopId") shopId: Long): Long
}
