package com.clara.ops.challenge.document_management_service_challenge.controller;

import com.clara.ops.challenge.document_management_service_challenge.dto.in.DocumentSearchFilters;
import com.clara.ops.challenge.document_management_service_challenge.dto.in.UploadDocumentRequest;
import com.clara.ops.challenge.document_management_service_challenge.dto.out.DocumentDownloadUrl;
import com.clara.ops.challenge.document_management_service_challenge.dto.out.PaginatedDocumentResponse;
import com.clara.ops.challenge.document_management_service_challenge.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/document-management")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * Accepts a multipart upload containing a file nd its associated metadata.
     *
     * @param file the file part of the multipart request
     * @param request validated inbound DTO carrying user, fileName, and tags from fields
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public void uploadDocument(@RequestPart("file") MultipartFile file,
                               @Valid @ModelAttribute UploadDocumentRequest request) {
        documentService.uploadDocument(request, file);
    }

    /**
     * Searches documents using optional filters provided in the request body.
     * All filters are optional. Results are paginated and default to {@code createdAt}
     * descending.
     *
     * @param filters inbound DTO with optional search criteria (user, fileName, tags)
     * @param pageable pagination and sort parameters
     * @return {@link PaginatedDocumentResponse} outbound DTO
     */
    @PostMapping("/search")
    public PaginatedDocumentResponse searchDocuments(
            @RequestBody DocumentSearchFilters filters,
            @PageableDefault(size=20, sort="createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return documentService.searchDocuments(filters, pageable);
    }

    /**
     * Returns a temporary pre-signed url for downloading the document identified by its UUID
     * @param documentId UUID of the document to download
     * @return {@link DocumentDownloadUrl} outbound DTO containing the pre-signed url
     */
    @GetMapping("/download/{documentId}")
    @ResponseStatus(HttpStatus.OK)
    public DocumentDownloadUrl downloadDocument(@PathVariable String documentId) {
        return documentService.getDownloadUrl(documentId);
    }
}
