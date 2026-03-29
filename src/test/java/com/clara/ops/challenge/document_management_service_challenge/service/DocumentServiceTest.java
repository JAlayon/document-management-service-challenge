package com.clara.ops.challenge.document_management_service_challenge.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.clara.ops.challenge.document_management_service_challenge.dto.UploadDocumentRequest;
import com.clara.ops.challenge.document_management_service_challenge.error.DocumentAlreadyExistsException;
import com.clara.ops.challenge.document_management_service_challenge.error.TooManyUploadsException;
import com.clara.ops.challenge.document_management_service_challenge.repository.DocumentRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;


@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private StorageService storageService;
    @Mock private UploadConcurrencyGuard concurrencyGuard;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(storageService, documentRepository, concurrencyGuard);
    }

    /**
     * Verifies that {@link DocumentService#uploadDocument} delegates entity creation to the adapter
     * and persists the result via the repository.
     */
    @Test
    void uploadDocument_persistsDocumentBuiltFromRequest() {
        var request = new UploadDocumentRequest("alice", "test.pdf", List.of("finance", "2024"));
        var file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[1024]);

        when(documentRepository.existsByUserAndFileName("alice", "test.pdf")).thenReturn(false);
        when(concurrencyGuard.tryAcquire()).thenReturn(true);
        when(storageService.uploadFile("alice", "test.pdf", file)).thenReturn("alice/test.pdf");
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        documentService.uploadDocument(request, file);

        verify(documentRepository)
                .save(
                        argThat(
                                doc ->
                                        doc.getUser().equals("alice")
                                                && doc.getFileName().equals("test.pdf")
                                                && doc.getStoragePath().equals("alice/test.pdf")
                                                && doc.getTags().containsAll(List.of("finance", "2024"))));
    }

    /**
     * Verifies that file storage is delegated to {@link StorageService} with the correct parameters.
     */
    @Test
    void uploadDocument_delegatesFileUploadToStorageService() {
        var request = new UploadDocumentRequest("bob", "doc.pdf", List.of());
        var file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[512]);

        when(documentRepository.existsByUserAndFileName("bob", "doc.pdf")).thenReturn(false);
        when(concurrencyGuard.tryAcquire()).thenReturn(true);
        when(storageService.uploadFile(anyString(), anyString(), any())).thenReturn("bob/doc.pdf");
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        documentService.uploadDocument(request, file);

        verify(storageService).uploadFile("bob", "doc.pdf", file);
    }

    /**
     * Verifies that a request is rejected with {@link TooManyUploadsException} when the concurrency
     * guard has no available slots.
     */
    @Test
    void uploadDocument_whenAtCapacity_throwsTooManyUploadsException() {
        var request = new UploadDocumentRequest("alice", "block.pdf", List.of());
        var file = new MockMultipartFile("file", "block.pdf", "application/pdf", new byte[512]);

        when(documentRepository.existsByUserAndFileName(anyString(), anyString())).thenReturn(false);
        when(concurrencyGuard.tryAcquire()).thenReturn(false);

        assertThatThrownBy(() -> documentService.uploadDocument(request, file))
                .isInstanceOf(TooManyUploadsException.class);

        verifyNoInteractions(storageService);
    }

    /**
     * Verifies that a duplicate upload (same user and name) is rejected with {@link
     * DocumentAlreadyExistsException} before any storage or concurrency guard interaction.
     */
    @Test
    void uploadDocument_whenDuplicate_throwsDocumentAlreadyExistsException() {
        var request = new UploadDocumentRequest("alice", "test.pdf", List.of());
        var file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[512]);

        when(documentRepository.existsByUserAndFileName("alice", "test.pdf")).thenReturn(true);

        assertThatThrownBy(() -> documentService.uploadDocument(request, file))
                .isInstanceOf(DocumentAlreadyExistsException.class);

        verifyNoInteractions(storageService, concurrencyGuard);
    }

}