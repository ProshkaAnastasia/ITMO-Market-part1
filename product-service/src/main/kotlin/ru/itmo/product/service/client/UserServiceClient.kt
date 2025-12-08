package ru.itmo.product.service.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(name = "user-service")
interface UserServiceClient {
    @GetMapping("/api/users/{id}")
    fun getUserById(@PathVariable id: Long): UserResponse
}

data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String
)
