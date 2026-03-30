package com.clara.ops.challenge.document_management_service_challenge.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.clara.ops.challenge.document_management_service_challenge.dto.in.DocumentSearchFilters;
import com.clara.ops.challenge.document_management_service_challenge.dto.out.DocumentDownloadUrl;
import com.clara.ops.challenge.document_management_service_challenge.dto.out.DocumentResponse;
import com.clara.ops.challenge.document_management_service_challenge.dto.out.Metadata;
import com.clara.ops.challenge.document_management_service_challenge.dto.out.PaginatedDocumentResponse;
import com.clara.ops.challenge.document_management_service_challenge.error.DocumentAlreadyExistsException;
import com.clara.ops.challenge.document_management_service_challenge.error.DocumentNotFoundException;
import com.clara.ops.challenge.document_management_service_challenge.error.DocumentTooLargeException;
import com.clara.ops.challenge.document_management_service_challenge.error.TooManyUploadsException;
import com.clara.ops.challenge.document_management_service_challenge.service.DocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit tests for {@link DocumentController} using the Spring MVC test slice.
 *
 * <p>All service-layer dependencies are replaced by Mockito mocks so that only the controller's
 * request mapping, Bean Validation, HTTP-status handling, and error-code contract are exercised.
 * Error responses are asserted against the {@link com.clara.ops.challenge.document_management_service_challenge.error.ApiErrorCode}
 * catalogue to ensure the JSON contract stays stable.
 */
@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    /** Spring MVC test driver provided by the {@code @WebMvcTest} slice. */
    @Autowired private MockMvc mockMvc;

    /** Jackson mapper used to serialise request bodies. */
    @Autowired private ObjectMapper objectMapper;

    /** Mock for the service layer injected into the controller under test. */
    @MockBean private DocumentService documentService;

    // -------------------------------------------------------------------------
    // Upload
    // -------------------------------------------------------------------------

    /**
     * Verifies that a well-formed multipart upload request returns HTTP 201 with no body.
     */
    @Test
    void uploadDocument_shouldReturnCreated_whenValidRequest() throws Exception {
        var file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[1024]);

        mockMvc.perform(
                        multipart("/document-management/upload")
                                .file(file)
                                .param("user", "alice")
                                .param("fileName", "test.pdf")
                                .param("tags", "tag1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").doesNotExist());
    }

    /**
     * Verifies that a missing {@code user} form field triggers HTTP 400 (DMS-005) due to Bean
     * Validation, and that the service is never called.
     */
    @Test
    void uploadDocument_shouldReturn400WithDms005_whenUserIsMissing() throws Exception {
        var file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[1024]);

        mockMvc.perform(
                        multipart("/document-management/upload")
                                .file(file)
                                .param("fileName", "test.pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DMS-005"))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(documentService);
    }

    /**
     * Verifies that a missing {@code fileName} form field triggers HTTP 400 (DMS-005) due to Bean
     * Validation, and that the service is never called.
     */
    @Test
    void uploadDocument_shouldReturn400WithDms005_whenFileNameIsMissing() throws Exception {
        var file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[1024]);

        mockMvc.perform(
                        multipart("/document-management/upload")
                                .file(file)
                                .param("user", "alice"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DMS-005"))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(documentService);
    }

    /**
     * Verifies that a {@link DocumentTooLargeException} thrown by the service is translated to HTTP
     * 413 (DMS-003) with the error code in the response body.
     */
    @Test
    void uploadDocument_shouldReturn413WithDms003_whenFileTooLarge() throws Exception {
        var file = new MockMultipartFile("file", "big.pdf", "application/pdf", new byte[1024]);
        doThrow(new DocumentTooLargeException("File too large"))
                .when(documentService).uploadDocument(any(), any());

        mockMvc.perform(
                        multipart("/document-management/upload")
                                .file(file)
                                .param("user", "alice")
                                .param("fileName", "big.pdf"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("DMS-003"))
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(jsonPath("$.message").value("File too large"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /**
     * Verifies that a {@link DocumentAlreadyExistsException} thrown by the service is translated to
     * HTTP 409 (DMS-002) with the error code in the response body.
     */
    @Test
    void uploadDocument_shouldReturn409WithDms002_whenDocumentAlreadyExists() throws Exception {
        var file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[1024]);
        doThrow(new DocumentAlreadyExistsException("Document already exists"))
                .when(documentService).uploadDocument(any(), any());

        mockMvc.perform(
                        multipart("/document-management/upload")
                                .file(file)
                                .param("user", "alice")
                                .param("fileName", "test.pdf"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DMS-002"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Document already exists"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /**
     * Verifies that a {@link TooManyUploadsException} thrown by the service is translated to HTTP
     * 429 (DMS-004) by {@link com.clara.ops.challenge.document_management_service_challenge.error.GlobalExceptionHandler}.
     */
    @Test
    void uploadDocument_shouldReturn429WithDms004_whenTooManyUploads() throws Exception {
        var file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[1024]);
        doThrow(new TooManyUploadsException("Maximum concurrent uploads reached. Please try again later."))
                .when(documentService).uploadDocument(any(), any());

        mockMvc.perform(
                        multipart("/document-management/upload")
                                .file(file)
                                .param("user", "alice")
                                .param("fileName", "test.pdf"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("DMS-004"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /**
     * Verifies that an unexpected {@link RuntimeException} from the service is translated to HTTP
     * 500 (DMS-006) without leaking the internal error message.
     */
    @Test
    void uploadDocument_shouldReturn500WithDms006_whenUnexpectedErrorOccurs() throws Exception {
        var file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[1024]);
        doThrow(new RuntimeException("Unexpected storage failure"))
                .when(documentService).uploadDocument(any(), any());

        mockMvc.perform(
                        multipart("/document-management/upload")
                                .file(file)
                                .param("user", "alice")
                                .param("fileName", "test.pdf"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("DMS-006"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------

    /**
     * Verifies that the search endpoint returns HTTP 200 with the paginated document list and
     * metadata when filters are provided.
     */
    @Test
    void searchDocuments_shouldReturnOk_withFilters() throws Exception {
        var docResponse =
                new DocumentResponse(
                        "id1",
                        "alice",
                        "report.pdf",
                        List.of("hr"),
                        1024L,
                        "application/pdf",
                        "2024-01-01T00:00:00");
        var metadata = new Metadata(0, 20, 1, 1, 1L);
        var response = new PaginatedDocumentResponse(metadata, List.of(docResponse));

        when(documentService.searchDocuments(any(DocumentSearchFilters.class), any(Pageable.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/document-management/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new DocumentSearchFilters("alice", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents[0].user").value("alice"))
                .andExpect(jsonPath("$.metadata.totalItems").value(1));
    }

    /**
     * Verifies that the search endpoint returns HTTP 200 with an empty result list when an empty
     * JSON body is provided.
     */
    @Test
    void searchDocuments_shouldReturnOk_withEmptyBody() throws Exception {
        var metadata = new Metadata(0, 20, 0, 0, 0L);
        var response = new PaginatedDocumentResponse(metadata, List.of());

        when(documentService.searchDocuments(any(DocumentSearchFilters.class), any(Pageable.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/document-management/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents").isEmpty());
    }

    // -------------------------------------------------------------------------
    // Download
    // -------------------------------------------------------------------------

    /**
     * Verifies that the download endpoint returns HTTP 200 with the pre-signed URL in the body.
     */
    @Test
    void downloadDocument_shouldReturnOk_withValidId() throws Exception {
        var docId = "a3d2ef10-1234-4567-abcd-00000000abcd";
        when(documentService.getDownloadUrl(docId))
                .thenReturn(new DocumentDownloadUrl("http://example.com/presigned"));

        mockMvc.perform(get("/document-management/download/{id}", docId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("http://example.com/presigned"));
    }

    /**
     * Verifies that a {@link DocumentNotFoundException} thrown by the service is translated to HTTP
     * 404 (DMS-001) with the error code in the response body.
     */
    @Test
    void downloadDocument_shouldReturn404WithDms001_whenDocumentNotFound() throws Exception {
        var docId = "a3d2ef10-1234-4567-abcd-00000000abcd";
        when(documentService.getDownloadUrl(docId))
                .thenThrow(new DocumentNotFoundException("Document not found with id: " + docId));

        mockMvc.perform(get("/document-management/download/{id}", docId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DMS-001"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Document not found with id: " + docId))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
