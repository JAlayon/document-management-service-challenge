package com.clara.ops.challenge.document_management_service_challenge.mapper;


import com.clara.ops.challenge.document_management_service_challenge.dto.in.UploadDocumentRequest;
import com.clara.ops.challenge.document_management_service_challenge.dto.out.DocumentDownloadUrl;
import com.clara.ops.challenge.document_management_service_challenge.entity.Document;
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
}
