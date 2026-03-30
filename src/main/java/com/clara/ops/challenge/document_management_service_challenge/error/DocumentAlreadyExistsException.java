package com.clara.ops.challenge.document_management_service_challenge.error;

/**
 * Thrown when a document with the same user and file name already exists.
 *
 * <p>Mapped to {@code 409 Conflict} ({@code DMS-002}) by {@link GlobalExceptionHandler}.
 */
public class DocumentAlreadyExistsException extends RuntimeException {
    public DocumentAlreadyExistsException(String message) {super(message);}
}
