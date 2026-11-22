package com.managify.exception;

import com.managify.dto.response.ApiResult;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Turns every exception into an {@link ApiResult}.
 *
 * <ul>
 *   <li>{@link AppException}: the status of its {@link ErrorCode} (400, 404, 409 ...)</li>
 *   <li>Validation and malformed input: 400, with field errors in {@code data}</li>
 *   <li>{@link EntityNotFoundException}: 404</li>
 *   <li>Database constraint violations: 409</li>
 *   <li>Anything else: 500 with a generic message</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxFileSize;

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResult<Map<String, Object>>> handleAppException(AppException ex) {
        if (ex.getStatus().is5xxServerError()) {
            log.error("AppException [code={}, status={}]: {}",
                    ex.getErrorCode().getCode(), ex.getStatus(), ex.getMessage(), ex);
        } else {
            log.warn("AppException [code={}, status={}]: {}",
                    ex.getErrorCode().getCode(), ex.getStatus(), ex.getMessage());
        }

        ApiResult<Map<String, Object>> body = ApiResult.<Map<String, Object>>builder()
                .success(false)
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode().name())
                .data(ex.getMetadata() == null || ex.getMetadata().isEmpty() ? null : ex.getMetadata())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleEntityNotFound(EntityNotFoundException ex) {
        log.warn("EntityNotFoundException: {}", ex.getMessage());
        return errorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResult<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return errorResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResult<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("Upload size exceeded: {}", ex.getMessage());
        return errorResponse(HttpStatus.BAD_REQUEST,
                "Upload size exceeded: files must not exceed " + maxFileSize + " each.");
    }

    // ── Validation (400) ─────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError fe ? fe.getField() : error.getObjectName();
            errors.put(fieldName, error.getDefaultMessage());
        });
        log.warn("Validation error: {}", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .errorCode(ErrorCode.BAD_REQUEST.name())
                        .data(errors)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResult<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return errorResponse(HttpStatus.BAD_REQUEST,
                "Required parameter '" + ex.getParameterName() + "' is missing");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResult<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(), ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        return errorResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        return errorResponse(HttpStatus.BAD_REQUEST, "Validation failed: " + ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResult<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return errorResponse(HttpStatus.BAD_REQUEST, "Malformed or unreadable request body");
    }

    // ── Conflicts (409) ──────────────────────────────────────────────────────

    /** A DB constraint (unique key, foreign key, not-null) rejected the write. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResult<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return errorResponse(HttpStatus.CONFLICT,
                "The request conflicts with existing data (duplicate value or record still in use)");
    }

    // ── Fallbacks ────────────────────────────────────────────────────────────

    /** Silently answer 404 for bot/scanner probes of non-existent static resources. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    /**
     * Spring MVC exceptions that already carry an HTTP status (405, 415 ...) keep it;
     * anything else becomes a 500 with a generic message so internals never leak.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleUnexpected(Exception ex) {
        if (ex instanceof ErrorResponse er) {
            HttpStatus status = HttpStatus.resolve(er.getStatusCode().value());
            log.warn("Request rejected [{}]: {}", er.getStatusCode(), ex.getMessage());
            return errorResponse(status != null ? status : HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        log.error("Unhandled exception", ex);
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.");
    }

    private ResponseEntity<ApiResult<Void>> errorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResult.error(message));
    }
}
