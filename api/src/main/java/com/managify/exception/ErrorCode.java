package com.managify.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Error codes for managify-api. Each code carries the HTTP status it maps to,
 * so {@link GlobalExceptionHandler} never re-derives it.
 *
 * <p>Code ranges:
 * <ul>
 *   <li>1000–1099  Resource not found (404)</li>
 *   <li>1100–1199  Business rule violations (409)</li>
 *   <li>1200–1299  Invalid input (400)</li>
 *   <li>1300–1399  Access (401, 403)</li>
 *   <li>1900–1999  Infrastructure (5xx)</li>
 * </ul>
 */
@Getter
public enum ErrorCode {

    // ── Resource not found (1000–1099) ───────────────────────────────────────
    RESOURCE_NOT_FOUND(1000, "Resource not found", HttpStatus.NOT_FOUND),
    PROJECT_NOT_FOUND(1001, "Project not found", HttpStatus.NOT_FOUND),
    TRACKER_ITEM_NOT_FOUND(1002, "Task or tasklet not found", HttpStatus.NOT_FOUND),
    WORKER_NOT_FOUND(1003, "Worker not found", HttpStatus.NOT_FOUND),

    // ── Business rule violations (1100–1199) ─────────────────────────────────
    CONFLICT(1100, "The request conflicts with the current state", HttpStatus.CONFLICT),
    ILLEGAL_STATUS_TRANSITION(1101, "Illegal status change", HttpStatus.CONFLICT),
    INVALID_HIERARCHY(1102, "That task/tasklet change is not allowed", HttpStatus.CONFLICT),
    DUPLICATE_PROJECT_NAME(1103, "A project with that name already exists", HttpStatus.CONFLICT),
    DUPLICATE_WORKER_EMAIL(1104, "A worker with that email already exists", HttpStatus.CONFLICT),
    WORKER_INACTIVE(1105, "The worker is not active", HttpStatus.CONFLICT),

    // ── Invalid input (1200–1299) ────────────────────────────────────────────
    BAD_REQUEST(1200, "Invalid request", HttpStatus.BAD_REQUEST),
    INVALID_DATES(1201, "Invalid dates", HttpStatus.BAD_REQUEST),

    // ── Access (1300–1399) ───────────────────────────────────────────────────
    UNAUTHENTICATED(1300, "Could not resolve user identity from token", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(1301, "You are not allowed to do this", HttpStatus.FORBIDDEN),

    // ── Infrastructure (1900–1999) ───────────────────────────────────────────
    UPSTREAM_UNAVAILABLE(1900, "A service this depends on did not answer", HttpStatus.BAD_GATEWAY);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
