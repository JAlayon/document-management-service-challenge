package com.clara.ops.challenge.document_management_service_challenge.error;

/**
 * Thrown when an uploaded file exceeds the configured maximum size.
 *
 * <p>Mapped to {@code 413 Payload Too Large} ({@code DMS-003}) by {@link GlobalExceptionHandler}.
 */
public class DocumentTooLargeException extends RuntimeException {
    public DocumentTooLargeException(String message) {super(message);}
}
