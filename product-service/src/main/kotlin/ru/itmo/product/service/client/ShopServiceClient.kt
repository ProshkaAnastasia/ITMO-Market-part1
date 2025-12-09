package ru.itmo.product.service.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import ru.itmo.product.model.dto.response.ShopResponse

@FeignClient(name = "shop-service")
interface ShopServiceClient {
    @GetMapping("/api/shops/{id}")
    fun getShopById(@PathVariable id: Long): ShopResponse
}
