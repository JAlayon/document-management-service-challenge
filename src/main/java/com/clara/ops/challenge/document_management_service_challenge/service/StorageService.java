package com.clara.ops.challenge.document_management_service_challenge.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;


@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private static final long UPLOAD_PART_SIZE = 5 * 1024 * 1024;
    @Value("${minio.bucket}")
    private String bucketName;

    private final MinioClient minioClient;


    public String uploadFile(String user, String fileName, MultipartFile file) {
        var objectPath = user + "/" + fileName;
        log.info("process=uploadFile, status=started, bucket={}, objectPath={}", bucketName, objectPath);
        try {
            ensureBucketExists();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectPath)
                    .stream(file.getInputStream(), file.getSize(), UPLOAD_PART_SIZE)
                    .build());
            log.info("process=uploadFile, status=completed, bucket={}, objectPath={}", bucketName, objectPath);
            return objectPath;
        } catch (Exception ex){
          log.error("process=uploadFile, status=failed, bucket={}, objectPath={}, " +
                  "error={}", bucketName, objectPath, ex.getMessage());
          throw new RuntimeException("Error uploading file to storage: " + ex.getMessage(), ex);
        }
    }

    public String getPresignedUrl(String documentId) {
        log.info("process=getPresignedUrl, status=started, documentId={}", documentId);
        try {
            var url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(documentId)
                    .method(Method.GET)
                    .expiry(1, TimeUnit.HOURS)
                    .build());

            log.info("process=getPresignedUrl, status=completed, documentId={}", documentId);
            return url;
        } catch (Exception ex) {
            log.error("process=getPresignedUrl, status=error,  documentId={}, error={}", documentId, ex.getMessage());
            throw new RuntimeException("Error generating presigned url: " + ex.getMessage(), ex);
        }
    }


    private void ensureBucketExists() throws Exception {
        var exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

}
