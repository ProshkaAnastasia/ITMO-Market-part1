package ru.itmo.order.service.client

@FeignClient(name = "product-service")
interface ProductServiceClient {
    @GetMapping("/api/products/{id}")
    fun getProductById(@PathVariable id: Long): ProductResponse
}