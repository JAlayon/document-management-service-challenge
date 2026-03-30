package com.clara.ops.challenge.document_management_service_challenge.error;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Central exception handler for all REST controllers.
 *
 * <p>Every non-2xx path produces a uniform {@link ApiErrorResponse} body so that callers always
 * receive a consistent structure regardless of which error was raised. HTTP status codes and
 * application error codes are derived from {@link ApiErrorCode} — the exceptions themselves carry
 * no HTTP-status annotations.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles requests for documents that do not exist.
     *
     * @param ex the exception raised by the service layer
     * @return {@code 404 Not Found} with code {@code DMS-001}
     */
    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleDocumentNotFound(DocumentNotFoundException ex) {
        return build(ApiErrorCode.DOCUMENT_NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles duplicate-document upload attempts for the same user and file name.
     *
     * @param ex the exception raised by the service layer
     * @return {@code 409 Conflict} with code {@code DMS-002}
     */
    @ExceptionHandler(DocumentAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleDocumentAlreadyExists(DocumentAlreadyExistsException ex) {
        return build(ApiErrorCode.DOCUMENT_ALREADY_EXISTS, ex.getMessage());
    }

    /**
     * Handles uploads whose payload exceeds the configured size limit.
     *
     * @param ex the exception raised by the service layer
     * @return {@code 413 Payload Too Large} with code {@code DMS-003}
     */
    @ExceptionHandler(DocumentTooLargeException.class)
    public ResponseEntity<ApiErrorResponse> handleDocumentTooLarge(DocumentTooLargeException ex) {
        return build(ApiErrorCode.DOCUMENT_TOO_LARGE, ex.getMessage());
    }

    /**
     * Handles upload rejections caused by the concurrent-upload limit being reached.
     *
     * @param ex the exception raised by the service layer
     * @return {@code 429 Too Many Requests} with code {@code DMS-004}
     */
    @ExceptionHandler(TooManyUploadsException.class)
    public ResponseEntity<ApiErrorResponse> handleTooManyUploads(TooManyUploadsException ex) {
        return build(ApiErrorCode.TOO_MANY_UPLOADS, ex.getMessage());
    }

    /**
     * Handles Bean Validation failures on request DTOs and multipart form fields.
     *
     * <p>All field errors are collected into a single comma-separated message so the client
     * receives the full list of violations in one response.
     *
     * @param ex the binding exception produced by Spring MVC
     * @return {@code 400 Bad Request} with code {@code DMS-005}
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationError(BindException ex) {
        var message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return build(ApiErrorCode.VALIDATION_ERROR, message);
    }

    /**
     * Catch-all handler for any unhandled exception.
     *
     * <p>The exception details are logged at {@code ERROR} level but are <em>not</em> included in
     * the response body to avoid leaking internal implementation details to callers.
     *
     * @param ex the unhandled exception
     * @return {@code 500 Internal Server Error} with code {@code DMS-006}
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedError(Exception ex) {
        log.error("process=handleUnexpectedError, error={}", ex.getMessage(), ex);
        return build(ApiErrorCode.INTERNAL_ERROR, ApiErrorCode.INTERNAL_ERROR.getDefaultMessage());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private ResponseEntity<ApiErrorResponse> build(ApiErrorCode errorCode, String message) {
        var body = new ApiErrorResponse(
                errorCode.getCode(),
                message,
                errorCode.getStatus().value(),
                LocalDateTime.now());
        return ResponseEntity.status(errorCode.getStatus()).body(body);
    }
}
