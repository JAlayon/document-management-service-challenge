package com.clara.ops.challenge.document_management_service_challenge.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

@Component
public class UploadConcurrencyGuard {

    private final Semaphore semaphore;

    public UploadConcurrencyGuard(
            @Value("${upload.max-concurrent}") int maxConcurrent) {
        this.semaphore = new Semaphore(maxConcurrent);
    }

    public boolean tryAcquire() {return semaphore.tryAcquire();}

    public void release() {semaphore.release();}
}
