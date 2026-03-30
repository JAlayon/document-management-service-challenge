package com.clara.ops.challenge.document_management_service_challenge.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * Uniform error response body returned by {@link GlobalExceptionHandler} for every non-2xx reply.
 *
 * <p>Example JSON:
 *
 * <pre>{@code
 * {
 *   "code":      "DMS-001",
 *   "message":   "Document not found with id: abc-123",
 *   "status":    404,
 *   "timestamp": "2024-06-01T12:00:00"
 * }
 * }</pre>
 *
 * @param code      stable application error code from {@link ApiErrorCode}
 * @param message   human-readable description of the error
 * @param status    HTTP status code (mirrors the response status line)
 * @param timestamp UTC instant at which the error was generated
 */
public record ApiErrorResponse(
        String code,
        String message,
        int status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime timestamp) {}
