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
import ru.itmo.market.model.dto.request.CreateCommentRequest
import ru.itmo.market.model.dto.request.UpdateCommentRequest
import ru.itmo.market.model.dto.request.RejectProductRequest
import ru.itmo.market.model.dto.response.CommentResponse
import ru.itmo.market.model.dto.response.ProductResponse
import ru.itmo.market.model.dto.response.PaginatedResponse
import ru.itmo.market.service.CommentService
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
        security = [SecurityRequirement(name = "bearer-jwt")]
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
                responseCode = "401",
                description = "UnauthorizedException: не авторизован"
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
        @RequestParam(defaultValue = "1")
        @Parameter(description = "Номер страницы (начиная с 1)", example = "1")
        page: Int,
        @RequestParam(defaultValue = "20")
        @Parameter(description = "Количество товаров на странице", example = "20")
        pageSize: Int
    ): ResponseEntity<PaginatedResponse<ProductResponse>> {
        return ResponseEntity.ok(moderationService.getPendingProducts(page, pageSize))
    }


    @GetMapping("/products/{id}")
    @Operation(
        summary = "Получить деталь товара на модерацию",
        description = "Возвращает полную информацию о товаре, ожидающем модерации. Доступно только модераторам и администраторам",
        security = [SecurityRequirement(name = "bearer-jwt")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Товар успешно получен",
                content = [Content(schema = Schema(implementation = ProductResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "UnauthorizedException: не авторизован"
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
        @PathVariable
        @Parameter(description = "ID товара на модерации", example = "1")
        id: Long
    ): ResponseEntity<ProductResponse> {
        return ResponseEntity.ok(moderationService.getPendingProductById(id))
    }


    @PostMapping("/products/{id}/approve")
    @Operation(
        summary = "Одобрить товар",
        description = "Одобряет товар на модерации, делая его видимым для покупателей. Доступно только модераторам и администраторам",
        security = [SecurityRequirement(name = "bearer-jwt")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Товар успешно одобрен",
                content = [Content(schema = Schema(implementation = ProductResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "UnauthorizedException: не авторизован"
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
        authentication: Authentication,
        @PathVariable
        @Parameter(description = "ID товара для одобрения", example = "1")
        id: Long
    ): ResponseEntity<ProductResponse> {
        val moderatorId = authentication.principal as Long
        return ResponseEntity.ok(moderationService.approvProduct(id, moderatorId))
    }


    @PostMapping("/products/{id}/reject")
    @Operation(
        summary = "Отклонить товар",
        description = "Отклоняет товар на модерации с указанием причины. Товар отправляется продавцу на доработку. Доступно только модераторам и администраторам",
        security = [SecurityRequirement(name = "bearer-jwt")]
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
                description = "BadRequestException: пустая причина отклонения"
            ),
            ApiResponse(
                responseCode = "401",
                description = "UnauthorizedException: не авторизован"
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
    fun rejectProduct(
        authentication: Authentication,
        @PathVariable
        @Parameter(description = "ID товара для отклонения", example = "1")
        id: Long,
        @Valid
        @RequestBody
        request: RejectProductRequest
    ): ResponseEntity<ProductResponse> {
        val moderatorId = authentication.principal as Long
        return ResponseEntity.ok(moderationService.rejectProduct(id, moderatorId, request.reason))
    }
}
