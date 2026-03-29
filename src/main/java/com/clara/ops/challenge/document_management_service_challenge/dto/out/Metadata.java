package com.clara.ops.challenge.document_management_service_challenge.dto.out;

public record Metadata(
        int currentPage,
        int itemsPerPage,
        int currentItems,
        int totalPages,
        long totalItems) {}
