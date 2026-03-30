package com.clara.ops.challenge.document_management_service_challenge.dto.in;

import java.util.List;

public record DocumentSearchFilters(
        String user,
        String fileName,
        List<String> tags
) {
    public static DocumentSearchFilters empty() {
        return new DocumentSearchFilters(null, null, null);
    }
}
