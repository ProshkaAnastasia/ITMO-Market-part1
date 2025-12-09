package ru.itmo.user.service.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import ru.itmo.user.model.dto.response.ProductResponse

@FeignClient(name = "user-service")
interface ProductServiceClient {
    @GetMapping("/api/products/{id}")
    fun getProductById(@PathVariable id: Long): ProductResponse
}
