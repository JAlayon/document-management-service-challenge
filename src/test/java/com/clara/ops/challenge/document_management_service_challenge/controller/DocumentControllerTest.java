package com.clara.ops.challenge.document_management_service_challenge.controller;

import static org.assertj.core.api.Assertions.*;
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
 * request mapping, Bean Validation, and HTTP-status handling are exercised.
 */
@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    /** Spring MVC test driver provided by the {@code @WebMvcTest} slice. */
    @Autowired private MockMvc mockMvc;

    /** Jackson mapper used to serialise request bodies. */
    @Autowired private ObjectMapper objectMapper;

    /** Mock for the service layer injected into the controller under test. */
    @MockBean private DocumentService documentService;

    /**
     * Verifies that a well-formed multipart upload request returns HTTP 201 with the document DTO
     * in the response body.
     */
    @Test
    void uploadDocument_shouldReturnCreated_whenValidRequest() throws Exception {
        var file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[1024]);
        var response =
                new DocumentResponse(
                        "uuid-1",
                        "alice",
                        "test.pdf",
                        List.of("tag1"),
                        1024L,
                        "application/pdf",
                        "2024-01-01T00:00:00");

        when(documentService.uploadDocument(any(), any())).thenReturn(response);

        mockMvc.perform(
                        multipart("/document-management/upload")
                                .file(file)
                                .param("user", "alice")
                                .param("fileName", "test.pdf")
                                .param("tags", "tag1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("uuid-1"))
                .andExpect(jsonPath("$.user").value("alice"))
                .andExpect(jsonPath("$.fileName").value("test.pdf"));
    }

    /**
     * Verifies that a missing {@code user} form field triggers HTTP 400 due to Bean Validation,
     * and that the service is never called.
     */
    @Test
    void uploadDocument_shouldReturn400_whenUserIsMissing() throws Exception {
        var file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[1024]);

        mockMvc.perform(
                        multipart("/document-management/upload")
                                .file(file)
                                .param("fileName", "test.pdf"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentService);
    }

    /**
     * Verifies that a missing {@code fileName} form field triggers HTTP 400 due to Bean Validation,
     * and that the service is never called.
     */
    @Test
    void uploadDocument_shouldReturn400_whenFileNameIsMissing() throws Exception {
        var file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[1024]);

        mockMvc.perform(
                        multipart("/document-management/upload")
                                .file(file)
                                .param("user", "alice"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentService);
    }

    /**
     * Verifies that a {@link DocumentTooLargeException} thrown by the service is translated to HTTP
     * 413 Payload Too Large.
     */
    @Test
    void uploadDocument_shouldReturn413_whenFileTooLarge() throws Exception {
        var file = new MockMultipartFile("file", "big.pdf", "application/pdf", new byte[1024]);
        when(documentService.uploadDocument(any(), any()))
                .thenThrow(new DocumentTooLargeException("File too large"));

        mockMvc.perform(
                        multipart("/document-management/upload")
                                .file(file)
                                .param("user", "alice")
                                .param("fileName", "big.pdf"))
                .andExpect(status().isPayloadTooLarge());
    }

    /**
     * Verifies that a {@link DocumentAlreadyExistsException} thrown by the service is translated to
     * HTTP 409 Conflict.
     */
    @Test
    void uploadDocument_shouldReturn409_whenDocumentAlreadyExists() throws Exception {
        var file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[1024]);
        when(documentService.uploadDocument(any(), any()))
                .thenThrow(new DocumentAlreadyExistsException("Document already exists"));

        mockMvc.perform(
                        multipart("/document-management/upload")
                                .file(file)
                                .param("user", "alice")
                                .param("fileName", "test.pdf"))
                .andExpect(status().isConflict());
    }

    /**
     * Verifies that a {@link TooManyUploadsException} thrown by the service propagates out of the
     * MockMvc request pipeline as an unhandled exception. Because no {@code @ResponseStatus}
     * annotation is present, Spring MVC does not map it to a specific HTTP status; the exception
     * wraps in a {@link org.springframework.web.util.NestedServletException} that bubbles up.
     */
    @Test
    void uploadDocument_shouldPropagateException_whenTooManyUploads() {
        var file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[1024]);
        when(documentService.uploadDocument(any(), any()))
                .thenThrow(new TooManyUploadsException("Too many concurrent uploads"));

        assertThatThrownBy(
                        () ->
                                mockMvc.perform(
                                        multipart("/document-management/upload")
                                                .file(file)
                                                .param("user", "alice")
                                                .param("fileName", "test.pdf")))
                .hasRootCauseInstanceOf(TooManyUploadsException.class);
    }

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
     * 404 Not Found.
     */
    @Test
    void downloadDocument_shouldReturn404_whenDocumentNotFound() throws Exception {
        var docId = "a3d2ef10-1234-4567-abcd-00000000abcd";
        when(documentService.getDownloadUrl(docId))
                .thenThrow(new DocumentNotFoundException("Document not found with id: " + docId));

        mockMvc.perform(get("/document-management/download/{id}", docId))
                .andExpect(status().isNotFound());
    }
}
