package com.clara.ops.challenge.document_management_service_challenge.error;

public class DocumentAlreadyExistsException extends RuntimeException {
    public DocumentAlreadyExistsException(String message) {super(message);}
}
