package org.example.web.advice;

import jakarta.servlet.http.HttpServletRequest;
import org.example.auth.exception.InvalidCredentialsException;
import org.example.auth.exception.UserAlreadyExistsException;
import org.example.exceptions.BusinessException;
import org.example.exceptions.PersistenceAccessException;
import org.example.exceptions.PersistenceEntityNotFoundException;
import org.example.web.dto.StandardApiErrorBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
/**
 * Gestionnaire global des exceptions REST avec format de réponse unifié.
 */
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Traite les exceptions métier et mappe un statut HTTP cohérent.
     *
     * @param ex exception métier levée
     * @param request requête HTTP en cours
     * @return réponse JSON normalisée
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.error("Erreur [METIER] sur {}: {}", request.getRequestURI(), ex.getMessage());
        HttpStatus status = mapBusinessStatus(ex.getErrorCode());
        return buildResponse(status, ex.getErrorCode(), ex.getMessage(), request.getRequestURI(), null);
    }

    /**
     * Traite les erreurs de validation des DTO de requête.
     *
     * @param ex exception de validation Spring
     * @param request requête HTTP en cours
     * @return réponse JSON normalisée avec détails de champs
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        log.error("Erreur [VALIDATION_DONNEES] sur {}: {}", request.getRequestURI(), ex.getMessage());
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

    /**
     * Traite le cas d'un utilisateur déjà existant à l'inscription.
     *
     * @param ex exception fonctionnelle levée
     * @param request requête HTTP en cours
     * @return réponse JSON normalisée
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(
            UserAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        log.error("Erreur [UTILISATEUR_DEJA_EXISTANT] sur {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", ex.getMessage(), request.getRequestURI(), null);
    }

    /**
     * Traite les erreurs d'identifiants invalides.
     *
     * @param ex exception d'authentification levée
     * @param request requête HTTP en cours
     * @return réponse JSON normalisée
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request
    ) {
        log.error("Erreur [IDENTIFIANTS_INVALIDES] sur {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage(), request.getRequestURI(), null);
    }

    /**
     * Traite les violations d'intégrité des données en persistence.
     *
     * @param ex exception d'intégrité levée par la couche data
     * @param request requête HTTP en cours
     * @return réponse JSON normalisée
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        log.error("Erreur [INTEGRITE_DES_DONNEES] sur {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(
                HttpStatus.CONFLICT,
                "DATA_INTEGRITY_VIOLATION",
                "Conflit d'intégrité des données.",
                request.getRequestURI(),
                null
        );
    }

    /**
     * Traite les cas d'entité absente côté persistence.
     *
     * @param ex exception d'entité introuvable
     * @param request requête HTTP en cours
     * @return réponse JSON normalisée
     */
    @ExceptionHandler(PersistenceEntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePersistenceEntityNotFound(
            PersistenceEntityNotFoundException ex,
            HttpServletRequest request
    ) {
        log.error("Erreur [ENTITE_PERSISTEE_INTROUVABLE] sur {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "PERSISTENCE_ENTITY_NOT_FOUND", ex.getMessage(), request.getRequestURI(), null);
    }

    /**
     * Traite les erreurs techniques d'accès aux données.
     *
     * @param ex exception d'accès persistence
     * @param request requête HTTP en cours
     * @return réponse JSON normalisée
     */
    @ExceptionHandler(PersistenceAccessException.class)
    public ResponseEntity<Map<String, Object>> handlePersistenceAccessException(
            PersistenceAccessException ex,
            HttpServletRequest request
    ) {
        log.error("Erreur [ACCES_PERSISTENCE] sur {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "PERSISTENCE_ACCESS_ERROR",
                "Erreur technique lors de l'accès aux données.",
                request.getRequestURI(),
                null
        );
    }

    /**
     * Traite toute exception non prévue.
     *
     * @param ex exception non catégorisée
     * @param request requête HTTP en cours
     * @return réponse JSON normalisée
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnhandledException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Erreur [ERREUR_INTERNE_SERVEUR] sur {}: {}", request.getRequestURI(), ex.getMessage(), ex);
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
