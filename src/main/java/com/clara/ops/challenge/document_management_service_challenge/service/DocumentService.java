package com.clara.ops.challenge.document_management_service_challenge.service;

import com.clara.ops.challenge.document_management_service_challenge.dto.in.UploadDocumentRequest;
import com.clara.ops.challenge.document_management_service_challenge.dto.out.DocumentDownloadUrl;
import com.clara.ops.challenge.document_management_service_challenge.entity.Document;
import com.clara.ops.challenge.document_management_service_challenge.error.DocumentAlreadyExistsException;
import com.clara.ops.challenge.document_management_service_challenge.error.DocumentNotFoundException;
import com.clara.ops.challenge.document_management_service_challenge.error.TooManyUploadsException;
import com.clara.ops.challenge.document_management_service_challenge.mapper.DocumentMapper;
import com.clara.ops.challenge.document_management_service_challenge.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final StorageService storageService;
    private final DocumentRepository documentRepository;
    private final UploadConcurrencyGuard uploadConcurrencyGuard;

    public void uploadDocument(UploadDocumentRequest request, MultipartFile file) {
        checkForDuplicate(request);
        checkCapacity(request);
        log.info("process=uploadDocument, status=started, user={}, fileName={}", request.user(), request.fileName());
        try {
            persistDocument(request, file);
            log.info("process=uploadDocument, status=completed, user={}, fileName={}, fileSize={}",
                    request.user(), request.fileName(), file.getSize());
        } catch (Exception ex) {
            log.error("process=uploadDocument, status=error, user={}, fileName={}, error={}",
                    request.user(), request.fileName(), ex.getMessage());
            throw ex;
        } finally {
            uploadConcurrencyGuard.release();
        }
    }


    public DocumentDownloadUrl getDownloadUrl(String documentId) {
        log.info("process=getDownloadUrl, status=started, documentId={}", documentId);
        try {
            var document = findDocumentById(documentId);
            var url = storageService.getPresignedUrl(document.getStoragePath());
            log.info("process=getDownloadUrl, status=completed, documentId={}, url={}", documentId, url);
            return DocumentMapper.toDocumentDownloadUrl(url);
        } catch (Exception ex) {
            log.error("process=getDownloadUrl, status=error, documentId={}, error={}", documentId, ex.getMessage());
            throw ex;
        }
    }

    private void persistDocument(UploadDocumentRequest request, MultipartFile file) {
        var storagePath = storageService.uploadFile(request.user(), request.fileName(), file);
        documentRepository.save(DocumentMapper.toDocument(request, storagePath, file));
    }

    private Document findDocumentById(String documentId) {
        return documentRepository
                .findById(UUID.fromString(documentId))
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with id: " + documentId));
    }

    private void checkForDuplicate(UploadDocumentRequest request) {
        if (documentRepository.existsByUserAndFileName(request.user(), request.fileName())) {
            log.error(
                    "process=checkForDuplicate, status=rejected, user={}, fileName={}, reason=duplicate",
                    request.user(), request.fileName()
            );
            throw new DocumentAlreadyExistsException("Document already exists");
        }
    }

    private void checkCapacity(UploadDocumentRequest request) {
        if (!uploadConcurrencyGuard.tryAcquire()) {
            log.error("process=checkCapacity, status=rejected, user={}, fileName={}, reason=capacityExceeded",
                    request.user(), request.fileName());
            throw new TooManyUploadsException("Maximum concurrent uploads reached. Please try again later.");
        }
    }
}
