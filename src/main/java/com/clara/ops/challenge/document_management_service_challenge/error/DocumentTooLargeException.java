package com.clara.ops.challenge.document_management_service_challenge.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
public class DocumentTooLargeException extends RuntimeException {
    public DocumentTooLargeException(String message) {super(message);}
}
