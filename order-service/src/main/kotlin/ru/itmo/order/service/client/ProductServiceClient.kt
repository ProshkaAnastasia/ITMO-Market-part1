package ru.itmo.order.service.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import ru.itmo.order.model.dto.response.ProductResponse

@FeignClient(name = "product-service")
interface ProductServiceClient {
    @GetMapping("/api/products/{id}")
    fun getProductById(@PathVariable id: Long): ProductResponse
}