package org.example.web.advice;

import jakarta.servlet.http.HttpServletRequest;
import org.example.auth.exception.InvalidCredentialsException;
import org.example.auth.exception.UserAlreadyExistsException;
import org.example.exceptions.BusinessException;
import org.example.exceptions.PersistenceAccessException;
import org.example.exceptions.PersistenceEntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.example.web.dto.StandardApiErrorBody;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        HttpStatus status = mapBusinessStatus(ex.getErrorCode());
        return buildResponse(status, ex.getErrorCode(), ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Échec de validation des champs.",
                request.getRequestURI(),
                fieldErrors
        );
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(
            UserAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "DATA_INTEGRITY_VIOLATION",
                "Conflit d'intégrité des données.",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(PersistenceEntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePersistenceEntityNotFound(
            PersistenceEntityNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, "PERSISTENCE_ENTITY_NOT_FOUND", ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(PersistenceAccessException.class)
    public ResponseEntity<Map<String, Object>> handlePersistenceAccessException(
            PersistenceAccessException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "PERSISTENCE_ACCESS_ERROR",
                "Erreur technique lors de l'accès aux données.",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnhandledException(
            Exception ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "Une erreur interne est survenue.",
                request.getRequestURI(),
                null
        );
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String errorCode,
            String message,
            String path,
            Map<String, String> details
    ) {
        Map<String, Object> body = StandardApiErrorBody.toMap(status, errorCode, message, path, details);
        return ResponseEntity.status(status).body(body);
    }

    private HttpStatus mapBusinessStatus(String errorCode) {
        return switch (errorCode) {
            case "PLAYER_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "DUPLICATE_PLAYER" -> HttpStatus.CONFLICT;
            case "SAME_PLAYER_MATCHUP", "INVALID_SCORE", "INVALID_NICKNAME", "INVALID_LEVEL" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
