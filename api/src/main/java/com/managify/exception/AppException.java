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

    public static AppException projectNotFound(Long id) {
        return of(ErrorCode.PROJECT_NOT_FOUND, "Project not found: " + id);
    }

    public static AppException trackerItemNotFound(Long projectId, Long itemId) {
        return of(ErrorCode.TRACKER_ITEM_NOT_FOUND,
                "Task or tasklet " + itemId + " not found in project " + projectId);
    }

    public static AppException workerNotFound(Long id) {
        return of(ErrorCode.WORKER_NOT_FOUND, "Worker not found: " + id);
    }

    public static AppException invalidHierarchy(String message) {
        return of(ErrorCode.INVALID_HIERARCHY, message);
    }

    public static AppException duplicateProjectName(String name) {
        return of(ErrorCode.DUPLICATE_PROJECT_NAME, "A project named '" + name + "' already exists");
    }

    public static AppException duplicateWorkerEmail(String email) {
        return of(ErrorCode.DUPLICATE_WORKER_EMAIL, "A worker with email " + email + " already exists");
    }

    public static AppException workerInactive(Long id) {
        return of(ErrorCode.WORKER_INACTIVE, "Worker " + id + " is not active and cannot be assigned");
    }

    public static AppException invalidDates(String message) {
        return of(ErrorCode.INVALID_DATES, message);
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
