package com.clara.ops.challenge.document_management_service_challenge.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom {@link HealthIndicator} that reports JVM heap memory usage.
 *
 * <p>Reports {@code UP} when heap usage is below the configured threshold, and {@code DOWN} when
 * usage meets or exceeds it. The response always includes raw memory figures so operators can
 * observe memory pressure trends over time.
 *
 * <p>Example response:
 *
 * <pre>{@code
 * "memory": {
 *   "status": "UP",
 *   "details": {
 *     "used":         "18 MB",
 *     "free":         "12 MB",
 *     "total":        "30 MB",
 *     "max":          "42 MB",
 *     "usagePercent": "42.9%"
 *   }
 * }
 * }</pre>
 */
@Component
public class MemoryHealthIndicator implements HealthIndicator {

    private static final long BYTES_PER_MB = 1024L * 1024L;

    /**
     * Heap usage ratio above which the indicator reports {@code DOWN}.
     * Defaults to {@code 0.90} (90 %) and is externalisable via
     * {@code health.memory.critical-threshold}.
     */
    private final double criticalThreshold;

    /**
     * @param criticalThreshold fraction of max heap ({@code 0.0}–{@code 1.0}) above which the
     *     indicator reports {@code DOWN}
     */
    public MemoryHealthIndicator(
            @Value("${health.memory.critical-threshold:0.90}") double criticalThreshold) {
        this.criticalThreshold = criticalThreshold;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Samples the JVM heap via {@link Runtime} and computes the usage ratio as
     * {@code usedMemory / maxMemory}. Status is {@code DOWN} when the ratio meets or exceeds
     * {@link #criticalThreshold}.
     */
    @Override
    public Health health() {
        Runtime runtime = Runtime.getRuntime();

        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long usedMemory = totalMemory - freeMemory;

        double usageRatio = (double) usedMemory / maxMemory;
        boolean healthy = usageRatio < criticalThreshold;

        return (healthy ? Health.up() : Health.down())
                .withDetail("used", formatMb(usedMemory))
                .withDetail("free", formatMb(freeMemory))
                .withDetail("total", formatMb(totalMemory))
                .withDetail("max", formatMb(maxMemory))
                .withDetail("usagePercent", String.format("%.1f%%", usageRatio * 100))
                .build();
    }

    private String formatMb(long bytes) {
        return (bytes / BYTES_PER_MB) + " MB";
    }
}
