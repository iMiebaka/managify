package com.managify.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * The single application exception. Throw it through a named factory;
 * {@link GlobalExceptionHandler} turns it into an {@code ApiResult} with the code's HTTP status.
 */
@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    private final HttpStatus status;

    /** Optional structured context returned in the response {@code data} field. May be null. */
    private final Map<String, Object> metadata;

    public AppException(ErrorCode errorCode, String message, Map<String, Object> metadata) {
        super(message);
        this.errorCode = errorCode;
        this.status = errorCode.getHttpStatus();
        this.metadata = metadata;
    }

    public static AppException of(ErrorCode code, String message) {
        return new AppException(code, message, null);
    }

    public static AppException of(ErrorCode code, String message, Map<String, Object> metadata) {
        return new AppException(code, message, metadata);
    }

    // ── Not found (404) ──────────────────────────────────────────────────────

    public static AppException notFound(String what, Object id) {
        return of(ErrorCode.RESOURCE_NOT_FOUND, what + " not found: " + id);
    }

    // ── Business rules (409) ─────────────────────────────────────────────────

    public static AppException conflict(String message) {
        return of(ErrorCode.CONFLICT, message);
    }

    public static AppException illegalStatusTransition(String subject, Object from, Object to) {
        return of(ErrorCode.ILLEGAL_STATUS_TRANSITION,
                "Cannot move " + subject + " from " + from + " to " + to,
                Map.of("from", String.valueOf(from), "to", String.valueOf(to)));
    }

    // ── Invalid input (400) ──────────────────────────────────────────────────

    public static AppException badRequest(String message) {
        return of(ErrorCode.BAD_REQUEST, message);
    }

    // ── Access (401, 403) ────────────────────────────────────────────────────

    public static AppException unauthenticated() {
        return of(ErrorCode.UNAUTHENTICATED, ErrorCode.UNAUTHENTICATED.getMessage());
    }

    public static AppException forbidden(String message) {
        return of(ErrorCode.FORBIDDEN, message);
    }

    // ── Infrastructure (5xx) ─────────────────────────────────────────────────

    public static AppException upstreamUnavailable(String service, Throwable cause) {
        AppException ex = of(ErrorCode.UPSTREAM_UNAVAILABLE, service + " could not be reached; try again shortly");
        ex.initCause(cause);
        return ex;
    }
}
