package ru.itmo.order.service.client

@FeignClient(name = "user-service")
interface UserServiceClient {
    @GetMapping("/api/users/{id}")
    fun getUserById(@PathVariable id: Long): UserResponse
}