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
@RequestMapping("/api/products/{productId}/comments")
@Tag(name = "Comments", description = "API для работы с комментариями")
class CommentController(
    private val commentService: CommentService
) {


    @GetMapping
    @Operation(
        summary = "Получить все комментарии товара",
        description = "Возвращает постраничный список комментариев для конкретного товара. Комментарии отсортированы по дате (новые сверху). Доступно для всех пользователей"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Список комментариев успешно получен",
                content = [Content(schema = Schema(implementation = PaginatedResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректные параметры пагинации"
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
    fun getProductComments(
        @PathVariable
        @Parameter(description = "ID товара", example = "1")
        productId: Long,
        @RequestParam(defaultValue = "1")
        @Parameter(description = "Номер страницы (начиная с 1)", example = "1")
        page: Int,
        @RequestParam(defaultValue = "20")
        @Parameter(description = "Количество комментариев на странице", example = "20")
        pageSize: Int
    ): ResponseEntity<PaginatedResponse<CommentResponse>> {
        return ResponseEntity.ok(commentService.getProductComments(productId, page, pageSize))
    }


    @PostMapping
    @Operation(
        summary = "Добавить комментарий к товару",
        description = "Создает новый комментарий/отзыв на товар. Один комментарий на пользователя на товар. Рейтинг от 1 до 5 звезд",
        security = [SecurityRequirement(name = "bearer-jwt")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Комментарий успешно создан",
                content = [Content(schema = Schema(implementation = CommentResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректный рейтинг (должен быть 1-5) или пустой текст"
            ),
            ApiResponse(
                responseCode = "401",
                description = "UnauthorizedException: не авторизован"
            ),
            ApiResponse(
                responseCode = "404",
                description = "ResourceNotFoundException: товар не найден или пользователь не найден"
            ),
            ApiResponse(
                responseCode = "409",
                description = "ConflictException: пользователь уже оставил комментарий на этот товар"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun createComment(
        authentication: Authentication,
        @PathVariable
        @Parameter(description = "ID товара", example = "1")
        productId: Long,
        @Valid @RequestBody request: CreateCommentRequest
    ): ResponseEntity<CommentResponse> {
        val userId = authentication.principal as Long
        return ResponseEntity.status(HttpStatus.CREATED).body(
            commentService.createComment(productId, userId, request.text, request.rating)
        )
    }


    @PutMapping("/{commentId}")
    @Operation(
        summary = "Обновить собственный комментарий",
        description = "Редактирует текст и рейтинг собственного комментария. Может редактировать только автор комментария",
        security = [SecurityRequirement(name = "bearer-jwt")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Комментарий успешно обновлен",
                content = [Content(schema = Schema(implementation = CommentResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректный рейтинг (должен быть 1-5) или пустой текст"
            ),
            ApiResponse(
                responseCode = "401",
                description = "UnauthorizedException: не авторизован"
            ),
            ApiResponse(
                responseCode = "403",
                description = "ForbiddenException: нет прав редактировать чужой комментарий"
            ),
            ApiResponse(
                responseCode = "404",
                description = "ResourceNotFoundException: товар не найден или комментарий не найден"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun updateComment(
        authentication: Authentication,
        @PathVariable
        @Parameter(description = "ID товара", example = "1")
        productId: Long,
        @PathVariable
        @Parameter(description = "ID комментария для редактирования", example = "1")
        commentId: Long,
        @Valid @RequestBody request: UpdateCommentRequest
    ): ResponseEntity<CommentResponse> {
        val userId = authentication.principal as Long
        return ResponseEntity.ok(
            commentService.updateComment(productId, commentId, userId, request.text, request.rating)
        )
    }


    @DeleteMapping("/{commentId}")
    @Operation(
        summary = "Удалить комментарий",
        description = "Полностью удаляет комментарий. Может удалять только автор комментария или модератор",
        security = [SecurityRequirement(name = "bearer-jwt")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "Комментарий успешно удален"
            ),
            ApiResponse(
                responseCode = "401",
                description = "UnauthorizedException: не авторизован"
            ),
            ApiResponse(
                responseCode = "403",
                description = "ForbiddenException: нет прав удалять чужой комментарий"
            ),
            ApiResponse(
                responseCode = "404",
                description = "ResourceNotFoundException: товар не найден или комментарий не найден"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun deleteComment(
        authentication: Authentication,
        @PathVariable
        @Parameter(description = "ID товара", example = "1")
        productId: Long,
        @PathVariable
        @Parameter(description = "ID комментария для удаления", example = "1")
        commentId: Long
    ): ResponseEntity<Unit> {
        val userId = authentication.principal as Long
        commentService.deleteComment(productId, commentId, userId)
        return ResponseEntity.noContent().build()
    }
}
