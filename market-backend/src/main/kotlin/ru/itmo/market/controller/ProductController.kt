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
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.itmo.market.model.dto.request.CreateProductRequest
import ru.itmo.market.model.dto.request.UpdateProductRequest
import ru.itmo.market.model.dto.response.InfiniteScrollResponse
import ru.itmo.market.model.dto.response.PaginatedResponse
import ru.itmo.market.model.dto.response.ProductResponse
import ru.itmo.market.service.ProductService

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "API для просмотра и управления товарами")
class ProductController(
    private val productService: ProductService
) {

    @GetMapping
    @Operation(
        summary = "Получить список одобренных товаров (пагинация)",
        description = "Возвращает постраничный список всех одобренных товаров. Total count и total pages передаются в заголовках ответа (X-Total-Count, X-Total-Pages)"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Список товаров успешно получен",
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
    fun getProducts(
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
        val response = productService.getApprovedProducts(page, pageSize)
        return ResponseEntity.ok()
            .header("X-Total-Count", response.totalElements.toString())
            .header("X-Total-Pages", response.totalPages.toString())
            .body(response)
    }

    @GetMapping("/infinite")
    @Operation(
        summary = "Получить товары для бесконечной прокрутки",
        description = "Возвращает товары без общего количества записей для infinite scroll"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Товары для infinite scroll успешно получены",
                content = [Content(schema = Schema(implementation = InfiniteScrollResponse::class))]
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
    fun getProductsInfinite(
        @RequestParam(defaultValue = "1")
        @Parameter(description = "Номер страницы (начиная с 1)", example = "1")
        @Min(1, message = "page должен быть больше 0")
        page: Int,
        @RequestParam(defaultValue = "20")
        @Parameter(description = "Количество товаров на странице", example = "20")
        @Min(1, message = "pageSize должен быть больше 0")
        @Max(50, message = "pageSize не может превышать 50")
        pageSize: Int
    ): ResponseEntity<InfiniteScrollResponse<ProductResponse>> {
        return ResponseEntity.ok(productService.getApprovedProductsInfinite(page, pageSize))
    }

    @GetMapping("/search")
    @Operation(
        summary = "Поиск товаров по ключевым словам",
        description = "Осуществляет полнотекстовый поиск товаров по названию и описанию. Возвращает только одобренные товары"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Результаты поиска успешно получены",
                content = [Content(schema = Schema(implementation = PaginatedResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректные ключевые слова или пагинация"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun searchProducts(
        @RequestParam
        @Parameter(description = "Ключевые слова для поиска", example = "ноутбук")
        @NotBlank(message = "Ключевые слова для поиска должны быть не пустые")
        keywords: String,
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
        return ResponseEntity.ok(productService.searchProducts(keywords, page, pageSize))
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Получить информацию о товаре",
        description = "Возвращает полную информацию о конкретном одобренном товаре. Доступно для всех пользователей"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Информация о товаре успешно получена",
                content = [Content(schema = Schema(implementation = ProductResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректный productId"
            ),
            ApiResponse(
                responseCode = "404",
                description = "ResourceNotFoundException: товар не найден"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun getProductById(
        @PathVariable
        @Parameter(description = "ID товара", example = "1")
        @Min(1, message = "productId должен быть больше 0")
        id: Long
    ): ResponseEntity<ProductResponse> {
        return ResponseEntity.ok(productService.getProductById(id))
    }

    @PostMapping
    @Operation(
        summary = "Создать новый товар",
        description = "Создает новый товар в магазине продавца. Товар автоматически попадает на модерацию"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Товар успешно создан",
                content = [Content(schema = Schema(implementation = ProductResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректные данные товара"
            ),
            ApiResponse(
                responseCode = "403",
                description = "ForbiddenException: только владелец может добавлять товары"
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
    fun createProduct(
        @RequestParam
        @Parameter(description = "ID продавца", example = "1")
        @Min(1, message = "sellerId должен быть больше 0")
        sellerId: Long,
        @Valid @RequestBody request: CreateProductRequest
    ): ResponseEntity<ProductResponse> {
        val response = productService.createProduct(
            name = request.name,
            description = request.description,
            price = request.price,
            imageUrl = request.imageUrl,
            shopId = request.shopId,
            sellerId = sellerId
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Обновить товар",
        description = "Редактирует товар. Может редактировать только владелец товара или администратор. Отправляет товар на повторную модерацию"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Товар успешно обновлен",
                content = [Content(schema = Schema(implementation = ProductResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректные данные обновления/некорректный productId/некорректный sellerId"
            ),
            ApiResponse(
                responseCode = "403",
                description = "ForbiddenException: нет прав на редактирование"
            ),
            ApiResponse(
                responseCode = "404",
                description = "ResourceNotFoundException: товар не найден"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun updateProduct(
        @PathVariable
        @Parameter(description = "ID товара", example = "1")
        @Min(1, message = "productId должен быть больше 0")
        id: Long,
        @RequestParam
        @Parameter(description = "ID продавца", example = "1")
        @Min(1, message = "sellerId должен быть больше 0")
        sellerId: Long,
        @Valid @RequestBody request: UpdateProductRequest
    ): ResponseEntity<ProductResponse> {
        return ResponseEntity.ok(productService.updateProduct(id, sellerId, request))
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Удалить товар",
        description = "Полностью удаляет товар. Может удалять только владелец товара или администратор"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "Товар успешно удален"
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректный productId/некорректный sellerId"
            ),
            ApiResponse(
                responseCode = "403",
                description = "ForbiddenException: нет прав на удаление"
            ),
            ApiResponse(
                responseCode = "404",
                description = "ResourceNotFoundException: товар не найден"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun deleteProduct(
        @PathVariable
        @Parameter(description = "ID товара", example = "1")
        @Min(1, message = "productId должен быть больше 0")
        id: Long,
        @RequestParam
        @Parameter(description = "ID продавца", example = "1")
        @Min(1, message = "sellerId должен быть больше 0")
        sellerId: Long
    ): ResponseEntity<Unit> {
        productService.deleteProduct(id, sellerId)
        return ResponseEntity.noContent().build()
    }
}
