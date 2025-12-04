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
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import ru.itmo.market.model.dto.request.UpdateProfileRequest
import ru.itmo.market.model.dto.response.UserResponse
import ru.itmo.market.service.UserService
import ru.itmo.market.exception.ForbiddenException


@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "API для управления профилями пользователей")
class UserController(
    private val userService: UserService
) {


    @GetMapping("/me")
    @Operation(
        summary = "Получить информацию о текущем пользователе",
        description = "Возвращает полную информацию о авторизованном пользователе",
        security = [SecurityRequirement(name = "bearer-jwt")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Информация о пользователе успешно получена",
                content = [Content(schema = Schema(implementation = UserResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "UnauthorizedException: не авторизован"
            ),
            ApiResponse(
                responseCode = "404",
                description = "ResourceNotFoundException: пользователь не найден"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun getCurrentUser(authentication: Authentication): ResponseEntity<UserResponse> {
        val userId = authentication.principal as Long
        return ResponseEntity.ok(userService.getCurrentUser(userId))
    }


    @PutMapping("/me")
    @Operation(
        summary = "Обновить свой профиль",
        description = "Редактирует информацию профиля авторизованного пользователя (email, имя, фамилия). Поля опциональны - передавайте только те, которые нужно изменить",
        security = [SecurityRequirement(name = "bearer-jwt")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Профиль успешно обновлен",
                content = [Content(schema = Schema(implementation = UserResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректные данные (невалидный email)"
            ),
            ApiResponse(
                responseCode = "401",
                description = "UnauthorizedException: не авторизован"
            ),
            ApiResponse(
                responseCode = "404",
                description = "ResourceNotFoundException: пользователь не найден"
            ),
            ApiResponse(
                responseCode = "409",
                description = "ConflictException: email уже используется другим пользователем"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun updateProfile(
        authentication: Authentication,
        @Valid
        @RequestBody
        request: UpdateProfileRequest
    ): ResponseEntity<UserResponse> {
        val userId = authentication.principal as Long
        return ResponseEntity.ok(
            userService.updateProfile(userId, request.email, request.firstName, request.lastName)
        )
    }


    @DeleteMapping("/me")
    @Operation(
        summary = "Удалить свой профиль",
        description = "Полностью удаляет профиль авторизованного пользователя. Это действие необратимо",
        security = [SecurityRequirement(name = "bearer-jwt")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "Профиль успешно удален"
            ),
            ApiResponse(
                responseCode = "401",
                description = "UnauthorizedException: не авторизован"
            ),
            ApiResponse(
                responseCode = "404",
                description = "ResourceNotFoundException: пользователь не найден"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun deleteProfile(authentication: Authentication): ResponseEntity<Unit> {
        val userId = authentication.principal as Long
        userService.deleteProfile(userId)
        return ResponseEntity.noContent().build()
    }


    @GetMapping("/{id}")
    @Operation(
        summary = "Получить информацию о пользователе (только для администраторов)",
        description = "Возвращает полную информацию о пользователе. Доступно только администраторам",
        security = [SecurityRequirement(name = "bearer-jwt")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Информация о пользователе успешно получена",
                content = [Content(schema = Schema(implementation = UserResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "UnauthorizedException: не авторизован"
            ),
            ApiResponse(
                responseCode = "403",
                description = "ForbiddenException: требуется роль администратора"
            ),
            ApiResponse(
                responseCode = "404",
                description = "ResourceNotFoundException: пользователь не найден"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun getUserById(
        authentication: Authentication,
        @PathVariable
        @Parameter(description = "ID пользователя", example = "1")
        id: Long
    ): ResponseEntity<UserResponse> {
        val roles = (authentication.details as? Map<*, *>)?.get("roles") as? Set<*> ?: emptySet<String>()
        if (!roles.contains("ADMIN")) {
            throw ForbiddenException("Only administrators can access user information")
        }
        return ResponseEntity.ok(userService.getUserById(id))
    }
}
