package com.clara.ops.challenge.document_management_service_challenge.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.clara.ops.challenge.document_management_service_challenge.dto.in.DocumentSearchFilters;
import com.clara.ops.challenge.document_management_service_challenge.dto.out.DocumentDownloadUrl;
import com.clara.ops.challenge.document_management_service_challenge.dto.out.PaginatedDocumentResponse;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * End-to-end integration tests for all document management endpoints.
 *
 * <p>Tests are intentionally ordered so that state built up in earlier tests (specifically the
 * document uploaded in {@link #uploadDocument_shouldReturnCreated()}) is available to later tests
 * that exercise search and download. The document ID captured from the search result is stored in
 * {@link #capturedDocumentId} and reused by the download tests.
 *
 * <p>Each test class boots a shared {@link org.testcontainers.containers.PostgreSQLContainer} and
 * {@link org.testcontainers.containers.MinIOContainer} via {@link BaseIT}.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("unchecked")
public class DocumentManagementIT extends BaseIT {

    private static final String USER = "alice";
    private static final String FILE_NAME = "integration-test-doc";
    private static final String TAG = "finance";

    /**
     * Document ID captured from the search response and reused by the download tests. The value is
     * populated by {@link #searchDocuments_shouldReturnDocument_filteredByUser()}.
     */
    private static String capturedDocumentId;

    @Autowired private TestRestTemplate restTemplate;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a multipart upload request with a small in-memory PDF content, the given user, file
     * name, and an optional list of tags.
     */
    private HttpEntity<MultiValueMap<String, Object>> buildUploadRequest(
            String user, String fileName, String... tags) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add(
                "file",
                new ByteArrayResource("%PDF-1.4 test content".getBytes()) {
                    @Override
                    public String getFilename() {
                        return fileName + ".pdf";
                    }
                });
        body.add("user", user);
        body.add("fileName", fileName);
        for (String tag : tags) {
            body.add("tags", tag);
        }
        return new HttpEntity<>(body, headers);
    }

    /** Builds a JSON request entity for the search endpoint. */
    private HttpEntity<DocumentSearchFilters> buildSearchRequest(DocumentSearchFilters filters) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(filters, headers);
    }

    // -------------------------------------------------------------------------
    // Upload — happy path
    // -------------------------------------------------------------------------

    /**
     * Verifies that a valid multipart upload returns {@code 201 Created} with no body. This test
     * must run first to seed the document used by the search and download tests.
     */
    @Test
    @Order(1)
    void uploadDocument_shouldReturnCreated() {
        var response =
                restTemplate.postForEntity(
                        "/document-management/upload",
                        buildUploadRequest(USER, FILE_NAME, TAG),
                        Void.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNull(response.getBody());
    }

    // -------------------------------------------------------------------------
    // Upload — error paths
    // -------------------------------------------------------------------------

    /**
     * Verifies that uploading the same user + file name combination a second time returns {@code
     * 409 Conflict} with error code {@code DMS-002}.
     */
    @Test
    @Order(2)
    void uploadDocument_shouldReturn409WithDms002_whenDocumentAlreadyExists() {
        var response =
                restTemplate.postForEntity(
                        "/document-management/upload",
                        buildUploadRequest(USER, FILE_NAME, TAG),
                        Map.class);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("DMS-002", response.getBody().get("code"));
        assertEquals(409, response.getBody().get("status"));
    }

    /**
     * Verifies that omitting the required {@code user} field returns {@code 400 Bad Request} with
     * error code {@code DMS-005}.
     */
    @Test
    @Order(3)
    void uploadDocument_shouldReturn400WithDms005_whenUserIsMissing() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add(
                "file",
                new ByteArrayResource("content".getBytes()) {
                    @Override
                    public String getFilename() {
                        return "doc.pdf";
                    }
                });
        body.add("fileName", FILE_NAME);

        var response =
                restTemplate.postForEntity(
                        "/document-management/upload",
                        new HttpEntity<>(body, headers),
                        Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("DMS-005", response.getBody().get("code"));
        assertEquals(400, response.getBody().get("status"));
    }

    /**
     * Verifies that omitting the required {@code fileName} field returns {@code 400 Bad Request}
     * with error code {@code DMS-005}.
     */
    @Test
    @Order(4)
    void uploadDocument_shouldReturn400WithDms005_whenFileNameIsMissing() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add(
                "file",
                new ByteArrayResource("content".getBytes()) {
                    @Override
                    public String getFilename() {
                        return "doc.pdf";
                    }
                });
        body.add("user", USER);

        var response =
                restTemplate.postForEntity(
                        "/document-management/upload",
                        new HttpEntity<>(body, headers),
                        Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("DMS-005", response.getBody().get("code"));
        assertEquals(400, response.getBody().get("status"));
    }

    /**
     * Verifies that uploading a file that exceeds 500 MB returns {@code 413 Payload Too Large} with
     * error code {@code DMS-003}.
     */
    @Test
    @Order(5)
    void uploadDocument_shouldReturn413WithDms003_whenFileTooLarge() throws IOException {
        long size = 501L * 1024 * 1024;
        Path tempFile = Files.createTempFile("large-doc", ".pdf");
        try (RandomAccessFile raf = new RandomAccessFile(tempFile.toFile(), "rw")) {
            raf.setLength(size);
        }

        try {
            var fileHeaders = new HttpHeaders();
            fileHeaders.setContentType(MediaType.APPLICATION_PDF);

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new HttpEntity<>(new FileSystemResource(tempFile), fileHeaders));
            body.add("user", USER);
            body.add("fileName", "too-large");

            var response =
                    restTemplate.postForEntity(
                            "/document-management/upload",
                            new HttpEntity<>(body, headers),
                            Map.class);

            assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
            assertEquals("DMS-003", response.getBody().get("code"));
            assertEquals(413, response.getBody().get("status"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    // -------------------------------------------------------------------------
    // Search — happy paths
    // -------------------------------------------------------------------------

    /**
     * Verifies that searching by {@code user} returns the previously uploaded document and captures
     * its ID for use by the download tests.
     */
    @Test
    @Order(6)
    void searchDocuments_shouldReturnDocument_filteredByUser() {
        var response =
                restTemplate.postForEntity(
                        "/document-management/search",
                        buildSearchRequest(new DocumentSearchFilters(USER, null, null)),
                        PaginatedDocumentResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        var documents = response.getBody().documents();
        assertFalse(documents.isEmpty());
        assertEquals(USER, documents.get(0).user());

        capturedDocumentId = documents.get(0).id();
        assertNotNull(capturedDocumentId);
    }

    /**
     * Verifies that searching by {@code fileName} returns the matching document.
     */
    @Test
    @Order(7)
    void searchDocuments_shouldReturnDocument_filteredByFileName() {
        var response =
                restTemplate.postForEntity(
                        "/document-management/search",
                        buildSearchRequest(new DocumentSearchFilters(null, FILE_NAME, null)),
                        PaginatedDocumentResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().documents().isEmpty());
        assertEquals(FILE_NAME, response.getBody().documents().get(0).fileName());
    }

    /**
     * Verifies that searching by {@code tags} returns documents that contain the given tag.
     */
    @Test
    @Order(8)
    void searchDocuments_shouldReturnDocument_filteredByTag() {
        var response =
                restTemplate.postForEntity(
                        "/document-management/search",
                        buildSearchRequest(new DocumentSearchFilters(null, null, List.of(TAG))),
                        PaginatedDocumentResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().documents().isEmpty());
        assertTrue(response.getBody().documents().get(0).tags().contains(TAG));
    }

    /**
     * Verifies that providing no filters returns all available documents with correct pagination
     * metadata.
     */
    @Test
    @Order(9)
    void searchDocuments_shouldReturnAllDocuments_whenNoFiltersProvided() {
        var response =
                restTemplate.postForEntity(
                        "/document-management/search",
                        buildSearchRequest(DocumentSearchFilters.empty()),
                        PaginatedDocumentResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        var metadata = response.getBody().metadata();
        assertNotNull(metadata);
        assertTrue(metadata.totalItems() >= 1);
        assertEquals(0, metadata.currentPage());
    }

    /**
     * Verifies that combining user and tag filters returns an empty list when no document matches
     * both criteria simultaneously.
     */
    @Test
    @Order(10)
    void searchDocuments_shouldReturnEmptyList_whenNoDocumentsMatchFilters() {
        var response =
                restTemplate.postForEntity(
                        "/document-management/search",
                        buildSearchRequest(
                                new DocumentSearchFilters(USER, null, List.of("nonexistent-tag"))),
                        PaginatedDocumentResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().documents().isEmpty());
        assertEquals(0, response.getBody().metadata().totalItems());
    }

    // -------------------------------------------------------------------------
    // Download — happy path
    // -------------------------------------------------------------------------

    /**
     * Verifies that requesting a download URL for an existing document returns {@code 200 OK} with a
     * non-blank presigned URL. Depends on {@link #searchDocuments_shouldReturnDocument_filteredByUser()}
     * having populated {@link #capturedDocumentId}.
     */
    @Test
    @Order(11)
    void downloadDocument_shouldReturnPresignedUrl_whenDocumentExists() {
        assertNotNull(capturedDocumentId, "capturedDocumentId must be set by the search test");

        var response =
                restTemplate.getForEntity(
                        "/document-management/download/" + capturedDocumentId,
                        DocumentDownloadUrl.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().url());
        assertFalse(response.getBody().url().isBlank());
    }

    // -------------------------------------------------------------------------
    // Download — error paths
    // -------------------------------------------------------------------------

    /**
     * Verifies that requesting a download URL for a well-formed but non-existent UUID returns
     * {@code 404 Not Found} with error code {@code DMS-001}.
     */
    @Test
    @Order(12)
    void downloadDocument_shouldReturn404WithDms001_whenDocumentNotFound() {
        var unknownId = UUID.randomUUID().toString();

        var response =
                restTemplate.getForEntity(
                        "/document-management/download/" + unknownId, Map.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("DMS-001", response.getBody().get("code"));
        assertEquals(404, response.getBody().get("status"));
        assertNotNull(response.getBody().get("message"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    /**
     * Verifies that requesting a download URL with a malformed (non-UUID) ID returns {@code 500
     * Internal Server Error} with error code {@code DMS-006}, since the UUID parsing failure is an
     * unhandled {@link IllegalArgumentException} caught by the fallback handler.
     */
    @Test
    @Order(13)
    void downloadDocument_shouldReturn500WithDms006_whenDocumentIdIsMalformed() {
        var response =
                restTemplate.getForEntity(
                        "/document-management/download/not-a-valid-uuid", Map.class);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("DMS-006", response.getBody().get("code"));
        assertEquals(500, response.getBody().get("status"));
    }
}
