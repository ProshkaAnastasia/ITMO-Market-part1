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
        page: Int,
        @RequestParam(defaultValue = "20")
        @Parameter(description = "Количество товаров на странице", example = "20")
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
        summary = "Получить список товаров (infinite scroll)",
        description = "Возвращает товары для бесконечной прокрутки. Поддерживает загрузку следующей страницы при скролле вниз"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Список товаров для infinite scroll успешно получен",
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
        page: Int,
        @RequestParam(defaultValue = "20")
        @Parameter(description = "Количество товаров на странице", example = "20")
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
                description = "BadRequestException: пустые keywords или некорректные параметры пагинации"
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
        keywords: String,
        @RequestParam(defaultValue = "1")
        @Parameter(description = "Номер страницы (начиная с 1)", example = "1")
        page: Int,
        @RequestParam(defaultValue = "20")
        @Parameter(description = "Количество товаров на странице", example = "20")
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
                responseCode = "404",
                description = "ResourceNotFoundException: товар не найден или не одобрен"
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
        id: Long
    ): ResponseEntity<ProductResponse> {
        return ResponseEntity.ok(productService.getProductById(id))
    }


    @PostMapping
    @Operation(
        summary = "Создать новый товар",
        description = "Создает новый товар и отправляет его на модерацию. Требуется авторизация",
        security = [SecurityRequirement(name = "bearer-jwt")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Товар успешно создан и отправлен на модерацию",
                content = [Content(schema = Schema(implementation = ProductResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректные данные товара (пустое название, отрицательная цена и т.д.)"
            ),
            ApiResponse(
                responseCode = "401",
                description = "UnauthorizedException: не авторизован"
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
        authentication: Authentication,
        @Valid @RequestBody request: CreateProductRequest
    ): ResponseEntity<ProductResponse> {
        val userId = authentication.principal as Long
        val response = productService.createProduct(
            name = request.name,
            description = request.description,
            price = request.price,
            imageUrl = request.imageUrl,
            shopId = request.shopId,
            sellerId = userId
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }


    @PutMapping("/{id}")
    @Operation(
        summary = "Обновить товар",
        description = "Редактирует товар. Может редактировать только владелец товара или администратор. Отправляет товар на повторную модерацию",
        security = [SecurityRequirement(name = "bearer-jwt")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Товар успешно обновлен и отправлен на модерацию",
                content = [Content(schema = Schema(implementation = ProductResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректные данные товара"
            ),
            ApiResponse(
                responseCode = "401",
                description = "UnauthorizedException: не авторизован"
            ),
            ApiResponse(
                responseCode = "403",
                description = "ForbiddenException: нет прав редактировать чужой товар"
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
        authentication: Authentication,
        @PathVariable
        @Parameter(description = "ID товара для редактирования", example = "1")
        id: Long,
        @Valid @RequestBody request: UpdateProductRequest
    ): ResponseEntity<ProductResponse> {
        val userId = authentication.principal as Long
        val roles = (authentication.details as? Map<*, *>)?.get("roles") as? Set<*> ?: emptySet<String>()
        return ResponseEntity.ok(productService.updateProduct(id, userId, roles as Set<String>, request))
    }


    @DeleteMapping("/{id}")
    @Operation(
        summary = "Удалить товар",
        description = "Полностью удаляет товар. Может удалять только владелец товара или администратор",
        security = [SecurityRequirement(name = "bearer-jwt")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "Товар успешно удален"
            ),
            ApiResponse(
                responseCode = "401",
                description = "UnauthorizedException: не авторизован"
            ),
            ApiResponse(
                responseCode = "403",
                description = "ForbiddenException: нет прав удалять чужой товар"
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
        authentication: Authentication,
        @PathVariable
        @Parameter(description = "ID товара для удаления", example = "1")
        id: Long
    ): ResponseEntity<Unit> {
        val userId = authentication.principal as Long
        val roles = (authentication.details as? Map<*, *>)?.get("roles") as? Set<*> ?: emptySet<String>()
        productService.deleteProduct(id, userId, roles as Set<String>)
        return ResponseEntity.noContent().build()
    }
}
