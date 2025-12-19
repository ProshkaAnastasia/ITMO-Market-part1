package ru.itmo.user.controller

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
import reactor.core.publisher.Mono
import ru.itmo.user.model.dto.request.UpdateProfileRequest
import ru.itmo.user.model.dto.response.UserResponse
import ru.itmo.user.service.UserService
import ru.itmo.user.exception.ForbiddenException

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "API для управления профилями пользователей")
class UserController(
    private val userService: UserService
) {

    @GetMapping("/me")
    @Operation(
        summary = "Получить информацию о текущем пользователе",
        description = "Возвращает полную информацию о пользователе"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Информация о пользователе успешно получена",
                content = [Content(schema = Schema(implementation = UserResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректный userId"
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
    fun getCurrentUser(
        @RequestParam
        @Parameter(description = "ID пользователя", example = "1")
        @Min(1, message = "userId должен быть больше 0")
        userId: Long
    ): Mono<ResponseEntity<UserResponse>> {
        return userService.getCurrentUser(userId)
            .map { ResponseEntity.ok(it) }
    }

    @PutMapping("/me")
    @Operation(
        summary = "Обновить профиль пользователя",
        description = "Редактирует информацию профиля пользователя (email, имя, фамилия). Поля опциональны"
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
                description = "BadRequestException: некорректный userId/некорректный email"
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
        @RequestParam
        @Parameter(description = "ID пользователя", example = "1")
        @Min(1, message = "userId должен быть больше 0")
        userId: Long,
        @Valid @RequestBody request: UpdateProfileRequest
    ): Mono<ResponseEntity<UserResponse>> {
        return userService.updateProfile(userId, request.email, request.firstName, request.lastName)
            .map { ResponseEntity.ok(it) }
    }

    @DeleteMapping("/me")
    @Operation(
        summary = "Удалить свой профиль",
        description = "Полностью удаляет профиль пользователя. Это действие необратимо"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "Профиль успешно удален"
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректный userId"
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
    fun deleteProfile(
        @RequestParam
        @Parameter(description = "ID пользователя", example = "1")
        @Min(1, message = "userId должен быть больше 0")
        userId: Long
    ): Mono<ResponseEntity<Unit>> {
        return userService.deleteProfile(userId)
            .then(Mono.just(ResponseEntity.noContent().build()))
    }

    @GetMapping("/{userId}")
    @Operation(
        summary = "Получить информацию о пользователе (администраторский доступ)",
        description = "Возвращает полную информацию о пользователе. Доступно только администраторам"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Информация о пользователе успешно получена",
                content = [Content(schema = Schema(implementation = UserResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректный adminId/некорректный userId"
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
        @RequestParam
        @Parameter(description = "ID администратора", example = "1")
        @Min(1, message = "adminId должен быть больше 0")
        adminId: Long,
        @PathVariable
        @Parameter(description = "ID пользователя", example = "1")
        @Min(1, message = "userId должен быть больше 0")
        userId: Long
    ): Mono<ResponseEntity<UserResponse>> {
        return userService.getUserById(adminId)
            .flatMap { admin ->
                if (!admin.roles.contains("ADMIN")) {
                    Mono.error(ForbiddenException("Only administrators can access user information"))
                } else {
                    userService.getUserById(userId)
                        .map { ResponseEntity.ok(it) }
                }
            }
    }
}
