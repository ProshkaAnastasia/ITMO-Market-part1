package ru.itmo.market.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.itmo.market.model.dto.request.CreateShopRequest
import ru.itmo.market.model.dto.request.UpdateShopRequest
import ru.itmo.market.model.dto.response.PaginatedResponse
import ru.itmo.market.model.dto.response.ProductResponse
import ru.itmo.market.model.dto.response.ShopResponse
import ru.itmo.market.service.ShopService

@RestController
@RequestMapping("/api/shops")
@Tag(name = "Shops", description = "API для просмотра и управления магазинами")
class ShopController(
    private val shopService: ShopService
) {

    @GetMapping
    @Operation(
        summary = "Получить список магазинов",
        description = "Возвращает постраничный список всех магазинов. Доступно для всех пользователей"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Список магазинов успешно получен",
                content = [Content(schema = Schema(implementation = PaginatedResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректные параметры пагинации"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun getShops(
        @RequestParam(defaultValue = "1")
        @Parameter(description = "Номер страницы (начиная с 1)", example = "1")
        @Min(1, message = "page должен быть больше 0")
        page: Int,
        @RequestParam(defaultValue = "20")
        @Parameter(description = "Количество магазинов на странице", example = "20")
        @Min(1, message = "pageSize должен быть больше 0")
        @Max(50, message = "pageSize не может превышать 50")
        pageSize: Int
    ): ResponseEntity<PaginatedResponse<ShopResponse>> {
        return ResponseEntity.ok(shopService.getAllShops(page, pageSize))
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Получить информацию о магазине",
        description = "Возвращает полную информацию о конкретном магазине, включая профиль и статистику. Доступно для всех пользователей"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Информация о магазине успешно получена",
                content = [Content(schema = Schema(implementation = ShopResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректный shopId"
            ),
            ApiResponse(
                responseCode = "404",
                description = "ResourceNotFoundException: магазин не найден"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun getShopById(
        @PathVariable
        @Parameter(description = "ID магазина", example = "1")
        @Min(1, message = "shopId должен быть больше 0")
        id: Long
    ): ResponseEntity<ShopResponse> {
        return ResponseEntity.ok(shopService.getShopById(id))
    }

    @GetMapping("/{id}/products")
    @Operation(
        summary = "Получить одобренные товары магазина",
        description = "Возвращает постраничный список всех одобренных товаров конкретного магазина. Доступно для всех пользователей"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Список товаров магазина успешно получен",
                content = [Content(schema = Schema(implementation = PaginatedResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректные параметры пагинации/некорректный id"
            ),
            ApiResponse(
                responseCode = "404",
                description = "ResourceNotFoundException: магазин не найден"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun getShopProducts(
        @PathVariable
        @Parameter(description = "ID магазина", example = "1")
        @Min(1, message = "shopId должен быть больше 0")
        id: Long,
        @RequestParam(defaultValue = "1")
        @Parameter(description = "Номер страницы (начиная с 1)", example = "1")
        @Min(1, message = "page должен быть больше 0")
        page: Int,
        @RequestParam(defaultValue = "20")
        @Parameter(description = "Количество товаров на странице", example = "20")
        @Min(1, message = "pageSize должен быть больше 0")
        @Max(50, message = "pageSize не может превышать 50")
        pageSize: Int
    ): ResponseEntity<PaginatedResponse<ProductResponse>> {
        return ResponseEntity.ok(shopService.getShopProducts(id, page, pageSize))
    }

    @PostMapping
    @Operation(
        summary = "Создать новый магазин",
        description = "Создает новый магазин для пользователя. Один пользователь может создать несколько магазинов"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Магазин успешно создан",
                content = [Content(schema = Schema(implementation = ShopResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректные данные магазина/некорректный userId"
            ),
            ApiResponse(
                responseCode = "409",
                description = "ConflictException: магазин с таким названием уже существует"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun createShop(
        @RequestParam
        @Parameter(description = "ID пользователя-продавца", example = "1")
        @Min(1, message = "userId должен быть больше 0")
        userId: Long,
        @Valid @RequestBody request: CreateShopRequest
    ): ResponseEntity<ShopResponse> {
        val response = shopService.createShop(
            sellerId = userId,
            name = request.name,
            description = request.description,
            avatarUrl = request.avatarUrl
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PutMapping("/{shopId}")
    @Operation(
        summary = "Обновить информацию магазина",
        description = "Редактирует информацию магазина. Может редактировать только владелец магазина или администратор"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Магазин успешно обновлен",
                content = [Content(schema = Schema(implementation = ShopResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректные данные магазина/некорректный shopId/некорректный userId"
            ),
            ApiResponse(
                responseCode = "403",
                description = "ForbiddenException: нет прав редактировать чужой магазин"
            ),
            ApiResponse(
                responseCode = "404",
                description = "ResourceNotFoundException: магазин не найден"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun updateShop(
        @PathVariable
        @Parameter(description = "ID магазина", example = "1")
        @Min(1, message = "shopId должен быть больше 0")
        shopId: Long,
        @RequestParam
        @Parameter(description = "ID пользователя", example = "1")
        @Min(1, message = "userId должен быть больше 0")
        userId: Long,
        @Valid @RequestBody request: UpdateShopRequest
    ): ResponseEntity<ShopResponse> {
        return ResponseEntity.ok(shopService.updateShop(shopId, userId, request.name, request.description, request.avatarUrl))
    }

    @DeleteMapping("/{shopId}")
    @Operation(
        summary = "Удалить магазин",
        description = "Полностью удаляет магазин"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "Магазин успешно удален"
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректный shopId"
            ),
            ApiResponse(
                responseCode = "403",
                description = "ForbiddenException: нет прав удалять чужой магазин"
            ),
            ApiResponse(
                responseCode = "404",
                description = "ResourceNotFoundException: магазин не найден"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun deleteShop(
        @PathVariable
        @Parameter(description = "ID магазина", example = "1")
        @Min(1, message = "shopId должен быть больше 0")
        shopId: Long,
        @RequestParam
        @Parameter(description = "ID пользователя", example = "1")
        @Min(1, message = "userId должен быть больше 0")
        userId: Long
    ): ResponseEntity<Unit> {
        shopService.deleteShop(shopId, userId)
        return ResponseEntity.noContent().build()
    }
}
