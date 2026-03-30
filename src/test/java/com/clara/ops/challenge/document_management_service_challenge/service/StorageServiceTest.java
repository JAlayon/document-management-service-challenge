package com.clara.ops.challenge.document_management_service_challenge.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link StorageService} verifying MinIO interactions and error-handling behaviour.
 *
 * <p>{@link MinioClient} is replaced by a Mockito mock; the {@code bucketName} field is injected
 * via reflection to avoid requiring a running Spring context.
 */
@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    /** Mocked MinIO client — replaces real network calls to object storage. */
    @Mock private MinioClient minioClient;

    /** Instance under test, constructed manually to avoid Spring context overhead. */
    private StorageService storageService;

    /** Sets up the service under test and injects the bucket name via reflection. */
    @BeforeEach
    void setUp() {
        storageService = new StorageService(minioClient);
        ReflectionTestUtils.setField(storageService, "bucketName", "test-bucket");
    }

    /**
     * Verifies that when the target bucket already exists the file is uploaded and the composed
     * object path ({@code user/fileName}) is returned.
     */
    @Test
    void uploadFile_shouldReturnObjectPath_whenBucketAlreadyExists() throws Exception {
        var file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[1024]);

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        var result = storageService.uploadFile("alice", "test.pdf", file);

        assertThat(result).isEqualTo("alice/test.pdf");
        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    /**
     * Verifies that when the target bucket does not yet exist the service creates it before
     * uploading the file.
     */
    @Test
    void uploadFile_shouldCreateBucketThenUpload_whenBucketDoesNotExist() throws Exception {
        var file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[1024]);

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        var result = storageService.uploadFile("alice", "test.pdf", file);

        assertThat(result).isEqualTo("alice/test.pdf");
        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    /**
     * Verifies that any exception thrown by the MinIO client during upload is caught and re-thrown
     * as a {@link RuntimeException} with a descriptive message.
     */
    @Test
    void uploadFile_shouldThrowRuntimeException_whenMinioThrows() throws Exception {
        var file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[1024]);

        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                .thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> storageService.uploadFile("alice", "test.pdf", file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error uploading file to storage");
    }

    /**
     * Verifies that a valid pre-signed URL is returned when the MinIO client responds successfully.
     */
    @Test
    void getPresignedUrl_shouldReturnUrl_whenMinioRespondsSuccessfully() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://example.com/presigned/test.pdf");

        var result = storageService.getPresignedUrl("alice/test.pdf");

        assertThat(result).isEqualTo("http://example.com/presigned/test.pdf");
    }

    /**
     * Verifies that any exception thrown by the MinIO client during URL generation is caught and
     * re-thrown as a {@link RuntimeException} with a descriptive message.
     */
    @Test
    void getPresignedUrl_shouldThrowRuntimeException_whenMinioThrows() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenThrow(new RuntimeException("minio error"));

        assertThatThrownBy(() -> storageService.getPresignedUrl("alice/test.pdf"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error generating presigned url");
    }
}
