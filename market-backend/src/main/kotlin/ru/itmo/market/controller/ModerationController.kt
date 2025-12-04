package ru.itmo.market.controller

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
import ru.itmo.market.model.dto.request.RejectProductRequest
import ru.itmo.market.model.dto.response.PaginatedResponse
import ru.itmo.market.model.dto.response.ProductResponse
import ru.itmo.market.service.ModerationService

@RestController
@RequestMapping("/api/moderation")
@Tag(name = "Moderation", description = "API для модерации товаров. Доступно только модераторам и администраторам")
class ModerationController(
    private val moderationService: ModerationService
) {

    @GetMapping("/products")
    @Operation(
        summary = "Получить список товаров на модерацию",
        description = "Возвращает постраничный список товаров, ожидающих модерации. Доступно только модераторам и администраторам",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Список товаров для модерации успешно получен",
                content = [Content(schema = Schema(implementation = PaginatedResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректные параметры пагинации или userId"
            ),
            ApiResponse(
                responseCode = "403",
                description = "ForbiddenException: нет прав доступа (требуется роль модератора)"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun getPendingProducts(
        @RequestParam
        @Parameter(description = "ID пользователя", example = "1")
        @Min(1, message = "userId должен быть больше 0")
        userId: Long,
        @RequestParam(defaultValue = "1")
        @Parameter(description = "Номер страницы (начиная с 1)", example = "1")
        @Min(1, message = "page должен быть больше 0")
        page: Int,
        @RequestParam(defaultValue = "20")
        @Parameter(description = "Количество товаров на странице", example = "20")
        @Min(1, message = "pageSize должен быть больше 0")
        pageSize: Int
    ): ResponseEntity<PaginatedResponse<ProductResponse>> {
        return ResponseEntity.ok(moderationService.getPendingProducts(page, pageSize))
    }

    @GetMapping("/products/{id}")
    @Operation(
        summary = "Получить товар на модерацию по ID",
        description = "Возвращает информацию о товаре, ожидающем модерации"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Товар успешно получен",
                content = [Content(schema = Schema(implementation = ProductResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректный productId/некорректный userId"
            ),
            ApiResponse(
                responseCode = "403",
                description = "ForbiddenException: нет прав доступа (требуется роль модератора)"
            ),
            ApiResponse(
                responseCode = "404",
                description = "ResourceNotFoundException: товар на модерации не найден"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun getPendingProductById(
        @RequestParam
        @Parameter(description = "ID пользователя", example = "1")
        @Min(1, message = "userId должен быть больше 0")
        userId: Long,
        @PathVariable
        @Parameter(description = "ID товара на модерации", example = "1")
        @Min(1, message = "productId должен быть больше 0")
        id: Long
    ): ResponseEntity<ProductResponse> {
        return ResponseEntity.ok(moderationService.getPendingProductById(id))
    }

    @PostMapping("/products/{id}/approve")
    @Operation(
        summary = "Одобрить товар",
        description = "Одобряет товар на модерации, делая его видимым для покупателей. Доступно только модераторам и администраторам"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Товар успешно одобрен",
                content = [Content(schema = Schema(implementation = ProductResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректный productId/некорректный userId"
            ),
            ApiResponse(
                responseCode = "403",
                description = "ForbiddenException: нет прав доступа (требуется роль модератора)"
            ),
            ApiResponse(
                responseCode = "404",
                description = "ResourceNotFoundException: товар на модерации не найден"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun approveProduct(
        @RequestParam
        @Parameter(description = "ID пользователя", example = "1")
        @Min(1, message = "userId должен быть больше 0")
        userId: Long,
        @PathVariable
        @Parameter(description = "ID товара для одобрения", example = "1")
        @Min(1, message = "productId должен быть больше 0")
        id: Long
    ): ResponseEntity<ProductResponse> {
        return ResponseEntity.ok(moderationService.approvProduct(id, userId))
    }

    @PostMapping("/products/{id}/reject")
    @Operation(
        summary = "Отклонить товар",
        description = "Отклоняет товар на модерации с указанием причины. Товар отправляется продавцу на доработку. Доступно только модераторам и администраторам"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Товар успешно отклонен",
                content = [Content(schema = Schema(implementation = ProductResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректный productId/некорректный userId или пустая причина"
            ),
            ApiResponse(
                responseCode = "403",
                description = "ForbiddenException: нет прав доступа (требуется роль модератора)"
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
    fun rejectProduct(
        @RequestParam
        @Parameter(description = "ID пользователя", example = "1")
        @Min(1, message = "userId должен быть больше 0")
        userId: Long,
        @PathVariable
        @Parameter(description = "ID товара для отклонения", example = "1")
        @Min(1, message = "productId должен быть больше 0")
        id: Long,
        @Valid @RequestBody request: RejectProductRequest
    ): ResponseEntity<ProductResponse> {
        return ResponseEntity.ok(moderationService.rejectProduct(id, userId, request.reason))
    }
}
