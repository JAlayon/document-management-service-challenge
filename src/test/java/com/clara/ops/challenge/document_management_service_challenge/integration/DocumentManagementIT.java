package com.clara.ops.challenge.document_management_service_challenge.integration;

import com.clara.ops.challenge.document_management_service_challenge.dto.out.DocumentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;


import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DocumentManagementIT extends BaseIT{

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void uploadDocument_shouldReturnCreated() {
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


    @Test
    void uploadDocument_shouldThrowException_whenDocumentExceeds500MB() throws IOException {
        var user = "alice";
        var filename = "big-file";

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        // 700 MB in bytes
        long size = 700 * 1024 * 1024;
        Path tempFile = Files.createTempFile("largeFile", ".pdf");
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(tempFile.toFile(), "rw")) {
            randomAccessFile.setLength(size);
        }

        try {
            var resourceFile = new FileSystemResource(tempFile);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_PDF);
            HttpEntity<FileSystemResource> filePart = new HttpEntity<>(resourceFile, httpHeaders);

            body.add("file", filePart);
            body.add("user", user);
            body.add("fileName", filename);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity("/document-management/upload", requestEntity, String.class);

            assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
            assertNotNull(response.getBody());
        } finally {
            Files.deleteIfExists(tempFile);
        }

    }
}
