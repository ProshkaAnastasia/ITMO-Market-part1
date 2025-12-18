package ru.itmo.user.exception

import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.server.ServerWebExchange
import ru.itmo.user.model.dto.response.ErrorResponse
import java.time.LocalDateTime

@ControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        val message = ex.constraintViolations
            .joinToString(", ") { it.message }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                message = message,
                timestamp = LocalDateTime.now(),
                path = exchange.request.path.value(),
                status = HttpStatus.BAD_REQUEST.value()
            ))
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleValidationException(
        ex: HandlerMethodValidationException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {

        val errors = ex.allErrors.map { error ->
            error.defaultMessage ?: "Validation error"
        }

        val errorMessage = formatErrors(errors)
        logger.warn("Validation error [{}]:{}",
            exchange.request.path.value(),
            errorMessage
        )

        return ResponseEntity(
            ErrorResponse(
                message = "Validation failed",
                errors = errors,
                timestamp = LocalDateTime.now(),
                path = exchange.request.path.value(),
                status = HttpStatus.BAD_REQUEST.value()
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(
        ex: MethodArgumentNotValidException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.map {
            "${it.field}: ${it.defaultMessage}"
        }

        val errorMessage = formatErrors(errors)
        logger.warn("Validation error [{}]:{}",
            exchange.request.path.value(),
            errorMessage
        )

        return ResponseEntity(
            ErrorResponse(
                message = "Validation failed",
                errors = errors,
                timestamp = LocalDateTime.now(),
                path = exchange.request.path.value(),
                status = HttpStatus.BAD_REQUEST.value()
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(
        ex: ResourceNotFoundException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {

        logger.warn("ResourceNotFoundException [{}]: {}",
            exchange.request.path.value(),
            ex.message
        )

        return ResponseEntity(
            ErrorResponse(
                message = ex.message,
                timestamp = LocalDateTime.now(),
                path = exchange.request.path.value(),
                status = HttpStatus.NOT_FOUND.value()
            ),
            HttpStatus.NOT_FOUND
        )
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(
        ex: UnauthorizedException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {

        logger.warn("UnauthorizedException [{}]: {}",
            exchange.request.path.value(),
            ex.message
        )

        return ResponseEntity(
            ErrorResponse(
                message = ex.message,
                timestamp = LocalDateTime.now(),
                path = exchange.request.path.value(),
                status = HttpStatus.UNAUTHORIZED.value()
            ),
            HttpStatus.UNAUTHORIZED
        )
    }

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbidden(
        ex: ForbiddenException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {

        logger.warn("ForbiddenException [{}]: {}",
            exchange.request.path.value(),
            ex.message
        )

        return ResponseEntity(
            ErrorResponse(
                message = ex.message,
                timestamp = LocalDateTime.now(),
                path = exchange.request.path.value(),
                status = HttpStatus.FORBIDDEN.value()
            ),
            HttpStatus.FORBIDDEN
        )
    }

    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequest(
        ex: BadRequestException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {

        logger.warn("BadRequestException [{}]: {}",
            exchange.request.path.value(),
            ex.message
        )

        return ResponseEntity(
            ErrorResponse(
                message = ex.message,
                timestamp = LocalDateTime.now(),
                path = exchange.request.path.value(),
                status = HttpStatus.BAD_REQUEST.value()
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(
        ex: ConflictException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {

        logger.warn("ConflictException [{}]: {}",
            exchange.request.path.value(),
            ex.message
        )

        return ResponseEntity(
            ErrorResponse(
                message = ex.message,
                timestamp = LocalDateTime.now(),
                path = exchange.request.path.value(),
                status = HttpStatus.CONFLICT.value()
            ),
            HttpStatus.CONFLICT
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGlobalException(
        ex: Exception,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {

        logger.error("${ex::class.simpleName} [{}]: {}",
            exchange.request.path.value(),
            ex.message
        )

        return ResponseEntity(
            ErrorResponse(
                message = "Internal server error",
                timestamp = LocalDateTime.now(),
                path = exchange.request.path.value(),
                status = HttpStatus.INTERNAL_SERVER_ERROR.value()
            ),
            HttpStatus.INTERNAL_SERVER_ERROR
        )
    }

    private fun formatErrors(errors: List<String>): String {
        return if (errors.isEmpty()) {
            ""
        } else if (errors.size == 1) {
            " ${errors[0]}"
        } else {
            "\n  - " + errors.joinToString("\n  - ")
        }
    }
}