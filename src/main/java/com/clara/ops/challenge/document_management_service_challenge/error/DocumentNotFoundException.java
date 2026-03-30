package com.clara.ops.challenge.document_management_service_challenge.error;

/**
 * Thrown when a document with the given identifier does not exist.
 *
 * <p>Mapped to {@code 404 Not Found} ({@code DMS-001}) by {@link GlobalExceptionHandler}.
 */
public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(String message) {super(message);}
}
