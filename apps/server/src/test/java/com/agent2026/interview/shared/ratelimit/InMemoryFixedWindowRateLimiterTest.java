package com.agent2026.interview.shared.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryFixedWindowRateLimiterTest {

    @Test
    void resetsQuotaAfterWindowExpires() {
        InMemoryFixedWindowRateLimiter limiter = limiter(2, 1_000, 10);

        assertThat(limiter.tryAcquire("127.0.0.1", 0)).isTrue();
        assertThat(limiter.tryAcquire("127.0.0.1", 10)).isTrue();
        assertThat(limiter.tryAcquire("127.0.0.1", 20)).isFalse();
        assertThat(limiter.tryAcquire("127.0.0.1", 1_000)).isTrue();
    }

    @Test
    void removesExpiredClientsAndBoundsTrackedKeys() {
        InMemoryFixedWindowRateLimiter limiter = limiter(1, 1_000, 2);

        assertThat(limiter.tryAcquire("10.0.0.1", 0)).isTrue();
        assertThat(limiter.tryAcquire("10.0.0.2", 0)).isTrue();
        assertThat(limiter.trackedClientCount()).isEqualTo(2);
        assertThat(limiter.tryAcquire("10.0.0.3", 500)).isFalse();

        assertThat(limiter.tryAcquire("10.0.0.3", 1_000)).isTrue();
        assertThat(limiter.trackedClientCount()).isEqualTo(1);
    }

    private InMemoryFixedWindowRateLimiter limiter(int requests, long windowMillis, int maxClients) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setRequestsPerWindow(requests);
        properties.setWindow(Duration.ofMillis(windowMillis));
        properties.setMaxTrackedClients(maxClients);
        return new InMemoryFixedWindowRateLimiter(properties);
    }
}
