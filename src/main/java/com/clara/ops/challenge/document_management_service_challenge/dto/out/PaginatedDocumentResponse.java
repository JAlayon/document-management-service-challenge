package com.clara.ops.challenge.document_management_service_challenge.dto.out;

import java.util.List;

public record PaginatedDocumentResponse(Metadata metadata, List<DocumentResponse> documents) {}
