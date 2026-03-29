package com.clara.ops.challenge.document_management_service_challenge.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UploadDocumentRequest(
        @NotBlank String user,
        @NotBlank String fileName,
        List<String> tags
) {

}
