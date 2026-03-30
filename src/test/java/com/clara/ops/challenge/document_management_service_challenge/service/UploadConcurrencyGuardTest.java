package com.clara.ops.challenge.document_management_service_challenge.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link UploadConcurrencyGuard} verifying the semaphore-based permit acquisition
 * and release behaviour without requiring a Spring context.
 */
class UploadConcurrencyGuardTest {

    /**
     * Verifies that {@link UploadConcurrencyGuard#tryAcquire()} returns {@code true} when the
     * guard still has available permits.
     */
    @Test
    void tryAcquire_shouldReturnTrue_whenPermitIsAvailable() {
        var guard = new UploadConcurrencyGuard(1);

        assertThat(guard.tryAcquire()).isTrue();
    }

    /**
     * Verifies that {@link UploadConcurrencyGuard#tryAcquire()} returns {@code false} once all
     * permits have been consumed.
     */
    @Test
    void tryAcquire_shouldReturnFalse_whenAllPermitsExhausted() {
        var guard = new UploadConcurrencyGuard(2);
        guard.tryAcquire();
        guard.tryAcquire();

        assertThat(guard.tryAcquire()).isFalse();
    }

    /**
     * Verifies that calling {@link UploadConcurrencyGuard#release()} after a successful acquire
     * makes the permit available again, allowing a subsequent acquire to succeed.
     */
    @Test
    void tryAcquire_shouldReturnTrue_afterRelease() {
        var guard = new UploadConcurrencyGuard(1);
        guard.tryAcquire();
        guard.release();

        assertThat(guard.tryAcquire()).isTrue();
    }

    /**
     * Verifies that a guard initialised with zero permits rejects all acquisition attempts
     * immediately, regardless of how many times it is called.
     */
    @Test
    void tryAcquire_shouldReturnFalse_whenGuardInitialisedWithZeroPermits() {
        var guard = new UploadConcurrencyGuard(0);

        assertThat(guard.tryAcquire()).isFalse();
        assertThat(guard.tryAcquire()).isFalse();
    }
}
