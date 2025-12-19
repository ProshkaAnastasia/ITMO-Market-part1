package ru.itmo.user

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.servers.Server
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.openfeign.EnableFeignClients


@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@OpenAPIDefinition(
    info = Info(
        title = "User-service API",
        version = "1.0.0",
        description = "API для работы с пользователями и магазинами"
    ),
    servers = [
        Server(url = "http://localhost:8080"),
        Server(url = "http://localhost:8081")
    ]
)
class UserServiceApplication


fun main(args: Array<String>) {
    runApplication<UserServiceApplication>(*args)
}