package com.clara.ops.challenge.document_management_service_challenge.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

/**
 * Unit tests for {@link MemoryHealthIndicator}.
 *
 * <p>The threshold is injected directly so tests are deterministic and independent of the actual
 * JVM heap state at test execution time.
 */
class MemoryHealthIndicatorTest {

    /**
     * Verifies that the indicator reports {@code UP} when the threshold is set to {@code 1.0}
     * (100%), which is always satisfied regardless of current memory usage.
     */
    @Test
    void health_shouldReturnUp_whenUsageBelowThreshold() {
        var indicator = new MemoryHealthIndicator(1.0);

        var health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKeys("used", "free", "total", "max", "usagePercent");
    }

    /**
     * Verifies that the indicator reports {@code DOWN} when the threshold is set to {@code 0.0}
     * (0%), which is never satisfied.
     */
    @Test
    void health_shouldReturnDown_whenUsageExceedsThreshold() {
        var indicator = new MemoryHealthIndicator(0.0);

        var health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKeys("used", "free", "total", "max", "usagePercent");
    }

    /**
     * Verifies that all detail values are present and formatted as MB strings or percentage strings.
     */
    @Test
    void health_shouldIncludeFormattedMemoryDetails() {
        var indicator = new MemoryHealthIndicator(1.0);

        var details = indicator.health().getDetails();

        assertThat(details.get("used").toString()).endsWith("MB");
        assertThat(details.get("free").toString()).endsWith("MB");
        assertThat(details.get("total").toString()).endsWith("MB");
        assertThat(details.get("max").toString()).endsWith("MB");
        assertThat(details.get("usagePercent").toString()).endsWith("%");
    }
}
