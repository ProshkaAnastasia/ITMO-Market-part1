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
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.itmo.market.model.dto.request.*
import ru.itmo.market.model.dto.response.*
import ru.itmo.market.service.*


@RestController
@RequestMapping("/api/cart")
@Tag(name = "Cart", description = "API для работы с корзиной")
class CartController(
    private val orderService: OrderService
) {


    @GetMapping
    @Operation(
        summary = "Получить текущую корзину",
        description = "Возвращает содержимое корзины пользователя со всеми товарами и ценами"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Корзина успешно получена",
                content = [Content(schema = Schema(implementation = OrderResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректный userId"
            ),
            ApiResponse(
                responseCode = "404",
                description = "ResourceNotFoundException: корзина не найдена"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun getCart(@RequestParam
                @Parameter(description = "ID пользователя", example = "1")
                @Min(1, message = "userId должен быть больше 0")
                userId: Long
    ): ResponseEntity<OrderResponse> {
        return ResponseEntity.ok(orderService.getCart(userId))
    }


    @PostMapping("/items")
    @Operation(
        summary = "Добавить товар в корзину",
        description = "Добавляет товар в корзину пользователя. Если товар уже в корзине, увеличивает количество"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Товар успешно добавлен в корзину",
                content = [Content(schema = Schema(implementation = OrderResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректное количество или ID товара"
            ),
            ApiResponse(
                responseCode = "404",
                description = "ResourceNotFoundException: товар или корзина не найдена"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun addToCart(
        @RequestParam
        @Parameter(description = "ID пользователя", example = "1")
        @Min(1, message = "userId должен быть больше 0")
        userId: Long,
        @Valid
        @RequestBody
        request: AddToCartRequest
    ): ResponseEntity<OrderResponse> {
        return ResponseEntity.ok(orderService.addToCart(userId, request.productId, request.quantity))
    }


    @PutMapping("/items/{itemId}")
    @Operation(
        summary = "Изменить количество товара в корзине",
        description = "Обновляет количество единиц товара в корзине. Количество должно быть больше 0"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Количество успешно изменено",
                content = [Content(schema = Schema(implementation = OrderResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректное количество/некорректный itemId/некорректный userId"
            ),
            ApiResponse(
                responseCode = "404",
                description = "ResourceNotFoundException: товар в корзине не найден"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun updateCartItem(
        @PathVariable
        @Parameter(description = "ID товара в корзине", example = "1")
        @Min(1, message = "itemId должен быть больше 0")
        itemId: Long,
        @RequestParam
        @Parameter(description = "ID пользователя", example = "1")
        @Min(1, message = "userId должен быть больше 0")
        userId: Long,
        @RequestBody request: UpdateQuantityRequest
    ): ResponseEntity<OrderResponse> {
        return ResponseEntity.ok(orderService.updateCartItemQuantity(userId, itemId, request.quantity))
    }


    @DeleteMapping("/items/{itemId}")
    @Operation(
        summary = "Удалить товар из корзины",
        description = "Полностью удаляет товар из корзины пользователя"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Товар успешно удален из корзины",
                content = [Content(schema = Schema(implementation = OrderResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректный itemId/некорректный userId"
            ),
            ApiResponse(
                responseCode = "404",
                description = "ResourceNotFoundException: товар в корзине не найден"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun removeFromCart(
        @RequestParam
        @Parameter(description = "ID пользователя", example = "1")
        @Min(1, message = "userId должен быть больше 0")
        userId: Long,
        @PathVariable
        @Parameter(description = "ID товара в корзине", example = "1")
        @Min(1, message = "itemId должен быть больше 0")
        itemId: Long
    ): ResponseEntity<OrderResponse> {
        return ResponseEntity.ok(orderService.removeFromCart(userId, itemId))
    }


    @DeleteMapping
    @Operation(
        summary = "Полностью очистить корзину",
        description = "Удаляет все товары из корзины пользователя"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "Корзина успешно очищена"
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректный userId"
            ),
            ApiResponse(
                responseCode = "404",
                description = "ResourceNotFoundException: корзина не найдена"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun clearCart(
        @RequestParam
        @Parameter(description = "ID пользователя", example = "1")
        @Min(1, message = "userId должен быть больше 0")
        userId: Long
    ): ResponseEntity<Unit> {
        orderService.clearCart(userId)
        return ResponseEntity.noContent().build()
    }
}
