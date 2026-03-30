package com.clara.ops.challenge.document_management_service_challenge.mapper;

import static org.assertj.core.api.Assertions.*;

import com.clara.ops.challenge.document_management_service_challenge.dto.in.UploadDocumentRequest;
import com.clara.ops.challenge.document_management_service_challenge.entity.Document;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Unit tests for {@link DocumentMapper} verifying correct DTO/entity transformation for every
 * static mapping method. No Spring context or mocks are required because the mapper contains only
 * pure static functions.
 */
class DocumentMapperTest {

    /**
     * Verifies that {@link DocumentMapper#toDocument} copies all fields from the request and the
     * multipart file into the resulting {@link Document} entity.
     */
    @Test
    void toDocument_shouldMapAllFieldsFromRequestAndFile() {
        var request = new UploadDocumentRequest("alice", "report.pdf", List.of("hr", "2024"));
        var file = new MockMultipartFile("file", "report.pdf", "application/pdf", new byte[2048]);

        var doc = DocumentMapper.toDocument(request, "alice/report.pdf", file);

        assertThat(doc.getUser()).isEqualTo("alice");
        assertThat(doc.getFileName()).isEqualTo("report.pdf");
        assertThat(doc.getTags()).containsExactlyInAnyOrder("hr", "2024");
        assertThat(doc.getStoragePath()).isEqualTo("alice/report.pdf");
        assertThat(doc.getFileSize()).isEqualTo(2048L);
        assertThat(doc.getFileType()).isEqualTo("application/pdf");
    }

    /**
     * Verifies that {@link DocumentMapper#toDocumentResponse} maps every field of a {@link
     * Document} entity to the corresponding field of the response DTO, including ID and timestamp
     * serialisation.
     */
    @Test
    void toDocumentResponse_shouldMapAllEntityFields() {
        var id = UUID.randomUUID();
        var now = LocalDateTime.now();
        var doc =
                Document.builder()
                        .id(id)
                        .user("alice")
                        .fileName("report.pdf")
                        .tags(List.of("hr"))
                        .storagePath("alice/report.pdf")
                        .fileSize(1024L)
                        .fileType("application/pdf")
                        .createdAt(now)
                        .build();

        var response = DocumentMapper.toDocumentResponse(doc);

        assertThat(response.id()).isEqualTo(id.toString());
        assertThat(response.user()).isEqualTo("alice");
        assertThat(response.fileName()).isEqualTo("report.pdf");
        assertThat(response.tags()).containsExactly("hr");
        assertThat(response.size()).isEqualTo(1024L);
        assertThat(response.type()).isEqualTo("application/pdf");
        assertThat(response.createdAt()).isEqualTo(now.toString());
    }

    /**
     * Verifies that {@link DocumentMapper#toDocumentDownloadUrl} wraps the raw URL string in the
     * correct DTO.
     */
    @Test
    void toDocumentDownloadUrl_shouldWrapUrlInDto() {
        var url = "http://example.com/presigned/test.pdf";

        var result = DocumentMapper.toDocumentDownloadUrl(url);

        assertThat(result.url()).isEqualTo(url);
    }

    /**
     * Verifies that {@link DocumentMapper#toPaginateDocumentSearch} converts the page content into
     * a list of response DTOs with correct field values.
     */
    @Test
    void toPaginateDocumentSearch_shouldMapPageContent() {
        var id = UUID.randomUUID();
        var doc =
                Document.builder()
                        .id(id)
                        .user("bob")
                        .fileName("file.pdf")
                        .tags(List.of("a", "b"))
                        .storagePath("bob/file.pdf")
                        .fileSize(512L)
                        .fileType("application/pdf")
                        .createdAt(LocalDateTime.now())
                        .build();
        Page<Document> page = new PageImpl<>(List.of(doc), PageRequest.of(0, 20), 1);

        var result = DocumentMapper.toPaginateDocumentSearch(page);

        assertThat(result.documents()).hasSize(1);
        assertThat(result.documents().get(0).user()).isEqualTo("bob");
        assertThat(result.documents().get(0).id()).isEqualTo(id.toString());
        assertThat(result.documents().get(0).tags()).containsExactlyInAnyOrder("a", "b");
    }

    /**
     * Verifies that {@link DocumentMapper#toPaginateDocumentSearch} calculates the pagination
     * metadata (page number, size, totals) correctly.
     */
    @Test
    void toPaginateDocumentSearch_shouldMapPaginationMetadata() {
        var doc =
                Document.builder()
                        .id(UUID.randomUUID())
                        .user("bob")
                        .fileName("doc.pdf")
                        .tags(List.of())
                        .storagePath("bob/doc.pdf")
                        .fileSize(256L)
                        .fileType("application/pdf")
                        .createdAt(LocalDateTime.now())
                        .build();
        // 11 total items, page size 5, requesting page 2 → 3 total pages
        Page<Document> page = new PageImpl<>(List.of(doc), PageRequest.of(2, 5), 11);

        var result = DocumentMapper.toPaginateDocumentSearch(page);

        assertThat(result.metadata().currentPage()).isEqualTo(2);
        assertThat(result.metadata().itemsPerPage()).isEqualTo(5);
        assertThat(result.metadata().currentItems()).isEqualTo(1);
        assertThat(result.metadata().totalPages()).isEqualTo(3);
        assertThat(result.metadata().totalItems()).isEqualTo(11L);
    }
}
