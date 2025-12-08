package ru.itmo.market.exception


import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.HandlerMethodValidationException
import ru.itmo.market.model.dto.response.ErrorResponse
import java.time.LocalDateTime


@ControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException): ResponseEntity<ErrorResponse> {
        val message = ex.constraintViolations
            .joinToString(", ") { it.message }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message))
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleValidationException(
        ex: HandlerMethodValidationException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {

        val errors = ex.allErrors.map { error ->
            error.defaultMessage ?: "Validation error"
        }

        val errorMessage = formatErrors(errors)
        logger.warn("Validation error [{}]:{}",
            request.getDescription(false).replace("uri=", ""),
            errorMessage
        )

        return ResponseEntity(
            ErrorResponse(
                message = "Validation failed",
                errors = errors,
                timestamp = LocalDateTime.now(),
                path = request.getDescription(false).replace("uri=", ""),
                status = HttpStatus.BAD_REQUEST.value()
            ),
            HttpStatus.BAD_REQUEST
        )
    }


    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.map { 
            "${it.field}: ${it.defaultMessage}" 
        }
        
        val errorMessage = formatErrors(errors)
        logger.warn("Validation error [{}]:{}", 
            request.getDescription(false).replace("uri=", ""), 
            errorMessage
        )
        
        return ResponseEntity(
            ErrorResponse(
                message = "Validation failed",
                errors = errors,
                timestamp = LocalDateTime.now(),
                path = request.getDescription(false).replace("uri=", ""),
                status = HttpStatus.BAD_REQUEST.value()
            ),
            HttpStatus.BAD_REQUEST
        )
    }


    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(
        ex: ResourceNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        
        logger.warn("ResourceNotFoundException [{}]: {}", 
            request.getDescription(false).replace("uri=", ""), 
            ex.message
        )
        
        return ResponseEntity(
            ErrorResponse(
                message = ex.message,
                timestamp = LocalDateTime.now(),
                path = request.getDescription(false).replace("uri=", ""),
                status = HttpStatus.NOT_FOUND.value()
            ),
            HttpStatus.NOT_FOUND
        )
    }


    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(
        ex: UnauthorizedException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        
        logger.warn("UnauthorizedException [{}]: {}", 
            request.getDescription(false).replace("uri=", ""), 
            ex.message
        )
        
        return ResponseEntity(
            ErrorResponse(
                message = ex.message,
                timestamp = LocalDateTime.now(),
                path = request.getDescription(false).replace("uri=", ""),
                status = HttpStatus.UNAUTHORIZED.value()
            ),
            HttpStatus.UNAUTHORIZED
        )
    }


    @ExceptionHandler(ForbiddenException::class)
    fun handleForbidden(
        ex: ForbiddenException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        
        logger.warn("ForbiddenException [{}]: {}", 
            request.getDescription(false).replace("uri=", ""), 
            ex.message
        )
        
        return ResponseEntity(
            ErrorResponse(
                message = ex.message,
                timestamp = LocalDateTime.now(),
                path = request.getDescription(false).replace("uri=", ""),
                status = HttpStatus.FORBIDDEN.value()
            ),
            HttpStatus.FORBIDDEN
        )
    }


    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequest(
        ex: BadRequestException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        
        logger.warn("BadRequestException [{}]: {}", 
            request.getDescription(false).replace("uri=", ""), 
            ex.message
        )
        
        return ResponseEntity(
            ErrorResponse(
                message = ex.message,
                timestamp = LocalDateTime.now(),
                path = request.getDescription(false).replace("uri=", ""),
                status = HttpStatus.BAD_REQUEST.value()
            ),
            HttpStatus.BAD_REQUEST
        )
    }


    @ExceptionHandler(ConflictException::class)
    fun handleConflict(
        ex: ConflictException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        
        logger.warn("ConflictException [{}]: {}", 
            request.getDescription(false).replace("uri=", ""), 
            ex.message
        )
        
        return ResponseEntity(
            ErrorResponse(
                message = ex.message,
                timestamp = LocalDateTime.now(),
                path = request.getDescription(false).replace("uri=", ""),
                status = HttpStatus.CONFLICT.value()
            ),
            HttpStatus.CONFLICT
        )
    }


    @ExceptionHandler(Exception::class)
    fun handleGlobalException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        
        logger.error("${ex::class.simpleName} [{}]: {}", 
            request.getDescription(false).replace("uri=", ""), 
            ex.message
        )
        
        return ResponseEntity(
            ErrorResponse(
                message = "Internal server error",
                timestamp = LocalDateTime.now(),
                path = request.getDescription(false).replace("uri=", ""),
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
