package com.clara.ops.challenge.document_management_service_challenge.integration;

import com.clara.ops.challenge.document_management_service_challenge.dto.out.DocumentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;


import static org.junit.jupiter.api.Assertions.assertEquals;

public class DocumentManagementIT extends BaseIT{

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void uploadDocument_ShouldReturnCreated() {
        var user = "alice";
        var filename = "test-file";

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add(
                "file",
                new ByteArrayResource("content of file".getBytes()) {
                    @Override
                    public String getFilename() {
                        return "test-file.pdf";
                    }
                });
        body.add("user", user);
        body.add("fileName", filename);
        body.add("tags", "tag1");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<DocumentResponse> response =
                restTemplate.postForEntity("/document-management/upload", requestEntity, DocumentResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(response.getBody().fileName(), filename);
    }
}
