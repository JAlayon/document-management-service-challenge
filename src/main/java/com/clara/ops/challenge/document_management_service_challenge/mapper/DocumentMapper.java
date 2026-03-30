package com.clara.ops.challenge.document_management_service_challenge.mapper;


import com.clara.ops.challenge.document_management_service_challenge.dto.in.UploadDocumentRequest;
import com.clara.ops.challenge.document_management_service_challenge.dto.out.DocumentDownloadUrl;
import com.clara.ops.challenge.document_management_service_challenge.dto.out.DocumentResponse;
import com.clara.ops.challenge.document_management_service_challenge.dto.out.Metadata;
import com.clara.ops.challenge.document_management_service_challenge.dto.out.PaginatedDocumentResponse;
import com.clara.ops.challenge.document_management_service_challenge.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public class DocumentMapper {

    public static Document toDocument(UploadDocumentRequest request,
                                      String storagePath,
                                      MultipartFile file) {
        return Document.builder()
                .user(request.user())
                .fileName(request.fileName())
                .tags(request.tags())
                .storagePath(storagePath)
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .build();
    }

    public static DocumentDownloadUrl toDocumentDownloadUrl(String url) {
        return new DocumentDownloadUrl(url);
    }

    public static DocumentResponse toDocumentResponse(Document document) {
        return new DocumentResponse(
                document.getId().toString(),
                document.getUser(),
                document.getFileName(),
                document.getTags(),
                document.getFileSize(),
                document.getFileType(),
                document.getCreatedAt().toString()
        );
    }

    public static PaginatedDocumentResponse toPaginateDocumentSearch(Page<Document> page) {
        var documents = page.getContent().stream()
                .map(DocumentMapper::toDocumentResponse).toList();
        var metadata = new Metadata(
                page.getNumber(),
                page.getSize(),
                page.getNumberOfElements(),
                page.getTotalPages(),
                page.getTotalElements()
        );
        return new PaginatedDocumentResponse(metadata, documents);
    }
}
