package com.clara.ops.challenge.document_management_service_challenge.error;

/**
 * Thrown when the concurrent-upload limit is reached and no permit can be acquired.
 *
 * <p>Mapped to {@code 429 Too Many Requests} ({@code DMS-004}) by {@link GlobalExceptionHandler}.
 */
public class TooManyUploadsException extends RuntimeException {
    public TooManyUploadsException(String message) {super(message);}
}
