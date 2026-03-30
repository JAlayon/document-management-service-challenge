package com.clara.ops.challenge.document_management_service_challenge.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Catalogue of all application-level error codes.
 *
 * <p>Each constant pairs a stable, human-readable code (e.g. {@code DMS-001}) with the HTTP status
 * and a default message that is surfaced in the {@link ApiErrorResponse}. Keeping the catalogue
 * here — rather than scattered across exception classes — makes it easy to audit all error
 * contracts in one place.
 */
@Getter
@RequiredArgsConstructor
public enum ApiErrorCode {

    /** The requested document does not exist. */
    DOCUMENT_NOT_FOUND("DMS-001", HttpStatus.NOT_FOUND, "Document not found"),

    /** A document with the same user and file name already exists. */
    DOCUMENT_ALREADY_EXISTS("DMS-002", HttpStatus.CONFLICT, "Document already exists for this user"),

    /** The uploaded file exceeds the maximum allowed size. */
    DOCUMENT_TOO_LARGE("DMS-003", HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds the maximum allowed size"),

    /** The server is already handling the maximum number of concurrent uploads. */
    TOO_MANY_UPLOADS("DMS-004", HttpStatus.TOO_MANY_REQUESTS, "Maximum concurrent uploads reached, please try again later"),

    /** One or more request fields failed Bean Validation. */
    VALIDATION_ERROR("DMS-005", HttpStatus.BAD_REQUEST, "Request validation failed"),

    /** An unexpected error occurred that is not covered by a specific code. */
    INTERNAL_ERROR("DMS-006", HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");

    /** Stable public code included in every error response body. */
    private final String code;

    /** HTTP status associated with this error. */
    private final HttpStatus status;

    /** Default human-readable message used when the caller does not supply a custom one. */
    private final String defaultMessage;
}
