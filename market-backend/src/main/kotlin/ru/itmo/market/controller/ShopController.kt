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
        page: Int,
        @RequestParam(defaultValue = "20")
        @Parameter(description = "Количество магазинов на странице", example = "20")
        pageSize: Int
    ): ResponseEntity<PaginatedResponse<ShopResponse>> {
        return ResponseEntity.ok(shopService.getAllShops(page, pageSize))
    }


    @GetMapping("/{id}")
    @Operation(
        summary = "Получить информацию о магазине",
        description = "Возвращает полную информацию о конкретном магазине включая профиль и статистику. Доступно для всех пользователей"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Информация о магазине успешно получена",
                content = [Content(schema = Schema(implementation = ShopResponse::class))]
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
                description = "BadRequestException: некорректные параметры пагинации"
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
        id: Long,
        @RequestParam(defaultValue = "1")
        @Parameter(description = "Номер страницы (начиная с 1)", example = "1")
        page: Int,
        @RequestParam(defaultValue = "20")
        @Parameter(description = "Количество товаров на странице", example = "20")
        pageSize: Int
    ): ResponseEntity<PaginatedResponse<ProductResponse>> {
        return ResponseEntity.ok(shopService.getShopProducts(id, page, pageSize))
    }


    @PostMapping
    @Operation(
        summary = "Создать новый магазин",
        description = "Создает новый магазин для авторизованного пользователя. Требуется авторизация. Один пользователь может создать несколько магазинов",
        security = [SecurityRequirement(name = "bearer-jwt")]
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
                description = "BadRequestException: некорректные данные магазина (пустое название и т.д.)"
            ),
            ApiResponse(
                responseCode = "401",
                description = "UnauthorizedException: не авторизован"
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
        authentication: Authentication,
        @Valid
        @RequestBody
        request: CreateShopRequest
    ): ResponseEntity<ShopResponse> {
        val userId = authentication.principal as Long
        val response = shopService.createShop(
            sellerId = userId,
            name = request.name,
            description = request.description,
            avatarUrl = request.avatarUrl
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }


    @PutMapping("/{id}")
    @Operation(
        summary = "Обновить информацию магазина",
        description = "Редактирует информацию магазина. Может редактировать только владелец магазина или администратор",
        security = [SecurityRequirement(name = "bearer-jwt")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Информация магазина успешно обновлена",
                content = [Content(schema = Schema(implementation = ShopResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректные данные магазина"
            ),
            ApiResponse(
                responseCode = "401",
                description = "UnauthorizedException: не авторизован"
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
                responseCode = "409",
                description = "ConflictException: магазин с таким названием уже существует"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun updateShop(
        authentication: Authentication,
        @PathVariable
        @Parameter(description = "ID магазина для редактирования", example = "1")
        id: Long,
        @Valid
        @RequestBody
        request: UpdateShopRequest
    ): ResponseEntity<ShopResponse> {
        val userId = authentication.principal as Long
        return ResponseEntity.ok(shopService.updateShop(id, userId, request.name, request.description, request.avatarUrl))
    }
}
