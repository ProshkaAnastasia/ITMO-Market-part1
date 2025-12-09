package ru.itmo.order.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.itmo.order.model.dto.request.CreateOrderRequest
import ru.itmo.order.model.dto.response.OrderResponse
import ru.itmo.order.model.dto.response.PaginatedResponse
import ru.itmo.order.service.OrderService

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "API для управления заказами")
class OrderController(
    private val orderService: OrderService
) {

    @GetMapping
    @Operation(
        summary = "Получить список заказов пользователя",
        description = "Возвращает постраничный список всех заказов пользователя. Заказы отсортированы по дате создания (новые сверху)"
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
                description = "BadRequestException: некорректные параметры пагинации или userId"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun getUserOrders(
        @RequestParam
        @Parameter(description = "ID пользователя", example = "1")
        @Min(1, message = "userId должен быть больше 0")
        userId: Long,
        @RequestParam(defaultValue = "1")
        @Parameter(description = "Номер страницы (начиная с 1)", example = "1")
        @Min(1, message = "page должен быть больше 0")
        page: Int,
        @RequestParam(defaultValue = "20")
        @Parameter(description = "Количество заказов на странице", example = "20")
        @Min(1, message = "pageSize должен быть больше 0")
        pageSize: Int
    ): ResponseEntity<PaginatedResponse<OrderResponse>> {
        return ResponseEntity.ok(orderService.getUserOrders(userId, page, pageSize))
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Получить детали заказа",
        description = "Возвращает полную информацию о конкретном заказе. Пользователь может просмотреть только свои заказы"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Детали заказа успешно получены",
                content = [Content(schema = Schema(implementation = OrderResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректный orderId/некорректный userId"
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
        @RequestParam
        @Parameter(description = "ID пользователя", example = "1")
        @Min(1, message = "userId должен быть больше 0")
        userId: Long,
        @PathVariable
        @Parameter(description = "ID заказа", example = "1")
        @Min(1, message = "orderId должен быть больше 0")
        id: Long
    ): ResponseEntity<OrderResponse> {
        return ResponseEntity.ok(orderService.getOrderById(id, userId))
    }

    @PostMapping
    @Operation(
        summary = "Оформить заказ из корзины",
        description = "Создает новый заказ из товаров в корзине. Корзина должна содержать хотя бы один товар. После успешного оформления корзина очищается"
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
                description = "BadRequestException: пустой адрес доставки/корзина пуста/некорректный userId"
            ),
            ApiResponse(
                responseCode = "404",
                description = "ResourceNotFoundException: корзина пуста или не найдена"
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
        @RequestParam
        @Parameter(description = "ID пользователя", example = "1")
        @Min(1, message = "userId должен быть больше 0")
        userId: Long,
        @Valid @RequestBody request: CreateOrderRequest
    ): ResponseEntity<OrderResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            orderService.createOrder(userId, request.deliveryAddress)
        )
    }
}
