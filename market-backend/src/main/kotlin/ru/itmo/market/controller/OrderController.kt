package ru.itmo.market.controller


import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import ru.itmo.market.model.dto.request.*
import ru.itmo.market.model.dto.response.*
import ru.itmo.market.service.*


@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "API для управления заказами")
class OrderController(
    private val orderService: OrderService
) {


    @GetMapping
    @Operation(
        summary = "Получить список заказов пользователя",
        description = "Возвращает постраничный список всех заказов авторизованного пользователя. Заказы отсортированы по дате создания (новые сверху)",
        security = [SecurityRequirement(name = "bearer-jwt")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Список заказов успешно получен",
                content = [Content(schema = Schema(implementation = PaginatedResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректные параметры пагинации"
            ),
            ApiResponse(
                responseCode = "401",
                description = "UnauthorizedException: не авторизован"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun getUserOrders(
        authentication: Authentication,
        @RequestParam(defaultValue = "1")
        @Parameter(description = "Номер страницы (начиная с 1)", example = "1")
        page: Int,
        @RequestParam(defaultValue = "20")
        @Parameter(description = "Количество заказов на странице", example = "20")
        pageSize: Int
    ): ResponseEntity<PaginatedResponse<OrderResponse>> {
        val userId = authentication.principal as Long
        return ResponseEntity.ok(orderService.getUserOrders(userId, page, pageSize))
    }


    @GetMapping("/{id}")
    @Operation(
        summary = "Получить детали заказа",
        description = "Возвращает полную информацию о конкретном заказе. Пользователь может просмотреть только свои заказы",
        security = [SecurityRequirement(name = "bearer-jwt")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Детали заказа успешно получены",
                content = [Content(schema = Schema(implementation = OrderResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "UnauthorizedException: не авторизован"
            ),
            ApiResponse(
                responseCode = "403",
                description = "ForbiddenException: нет прав просмотреть чужой заказ"
            ),
            ApiResponse(
                responseCode = "404",
                description = "ResourceNotFoundException: заказ не найден"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun getOrderById(
        authentication: Authentication,
        @PathVariable
        @Parameter(description = "ID заказа", example = "1")
        id: Long
    ): ResponseEntity<OrderResponse> {
        val userId = authentication.principal as Long
        return ResponseEntity.ok(orderService.getOrderById(id, userId))
    }


    @PostMapping
    @Operation(
        summary = "Оформить заказ из корзины",
        description = "Создает новый заказ из товаров в корзине. Корзина должна содержать хотя бы один товар. После успешного оформления корзина очищается",
        security = [SecurityRequirement(name = "bearer-jwt")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Заказ успешно оформлен",
                content = [Content(schema = Schema(implementation = OrderResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: пустой адрес доставки или корзина пуста"
            ),
            ApiResponse(
                responseCode = "401",
                description = "UnauthorizedException: не авторизован"
            ),
            ApiResponse(
                responseCode = "404",
                description = "ResourceNotFoundException: корзина или пользователь не найден"
            ),
            ApiResponse(
                responseCode = "409",
                description = "ConflictException: в корзине закончились товары (недостаточно запасов)"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun createOrder(
        authentication: Authentication,
        @Valid
        @RequestBody
        request: CreateOrderRequest
    ): ResponseEntity<OrderResponse> {
        val userId = authentication.principal as Long
        return ResponseEntity.status(HttpStatus.CREATED).body(
            orderService.createOrder(userId, request.deliveryAddress)
        )
    }
}
