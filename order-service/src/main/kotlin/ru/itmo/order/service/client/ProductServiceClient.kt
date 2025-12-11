package ru.itmo.order.service.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import ru.itmo.order.exception.ServiceUnavailableException
import ru.itmo.order.model.dto.response.ProductResponse

@FeignClient(
    name = "product-service",
    fallback = ProductServiceClientFallback::class
)
interface ProductServiceClient {
    @GetMapping("/api/products/{id}")
    fun getProductById(@PathVariable id: Long): ProductResponse
}

@Component
class ProductServiceClientFallback : ProductServiceClient {
    override fun getProductById(id: Long): ProductResponse {
        throw ServiceUnavailableException(
            "Product service is temporarily unavailable. Please try again later."
        )
    }
}
