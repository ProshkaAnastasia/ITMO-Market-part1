package ru.itmo.market.controller


import io.swagger.v3.oas.annotations.Operation
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
import ru.itmo.market.model.dto.request.LoginRequest
import ru.itmo.market.model.dto.request.RegisterRequest
import ru.itmo.market.model.dto.request.RefreshTokenRequest
import ru.itmo.market.model.dto.response.TokenResponse
import ru.itmo.market.model.dto.response.UserResponse
import ru.itmo.market.service.AuthService
import ru.itmo.market.service.UserService


@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "API для аутентификации и авторизации")
class AuthController(
    private val authService: AuthService,
    private val userService: UserService
) {


    @PostMapping("/register")
    @Operation(
        summary = "Регистрация нового пользователя",
        description = "Создает новый аккаунт и возвращает JWT токены для доступа"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Пользователь успешно зарегистрирован",
                content = [Content(schema = Schema(implementation = TokenResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректные данные (email, пароль)"
            ),
            ApiResponse(
                responseCode = "409",
                description = "ConflictException: пользователь с таким email уже существует"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<TokenResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request))
    }


    @PostMapping("/login")
    @Operation(
        summary = "Вход в систему",
        description = "Аутентифицирует пользователя по email и пароля, возвращает JWT токены"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Успешная аутентификация",
                content = [Content(schema = Schema(implementation = TokenResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: некорректные данные (пустой email или пароль)"
            ),
            ApiResponse(
                responseCode = "401",
                description = "UnauthorizedException: неверные учетные данные"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<TokenResponse> {
        return ResponseEntity.ok(authService.login(request))
    }


    @PostMapping("/refresh")
    @Operation(
        summary = "Обновить access token",
        description = "Использует refresh token для получения нового access token без повторной аутентификации"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Token успешно обновлен",
                content = [Content(schema = Schema(implementation = TokenResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "BadRequestException: refresh token пустой или некорректный формат"
            ),
            ApiResponse(
                responseCode = "401",
                description = "UnauthorizedException: refresh token истекший или невалидный"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            )
        ]
    )
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<TokenResponse> {
        return ResponseEntity.ok(authService.refresh(request.refreshToken))
    }
}
