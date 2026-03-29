package com.clara.ops.challenge.document_management_service_challenge.dto.out;

import java.util.List;

public record DocumentResponse(
        String id,
        String user,
        String fileName,
        List<String> tags,
        Long size,
        String type,
        String createdAt) {}
