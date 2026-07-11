package com.agent2026.interview.shared.ratelimit;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryFixedWindowRateLimiter {

    private final int requestsPerWindow;
    private final long windowMillis;
    private final int maxTrackedClients;
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final AtomicLong nextCleanupAt = new AtomicLong(0);

    public InMemoryFixedWindowRateLimiter(RateLimitProperties properties) {
        this.requestsPerWindow = requirePositive(properties.getRequestsPerWindow(), "requests-per-window");
        this.maxTrackedClients = requirePositive(properties.getMaxTrackedClients(), "max-tracked-clients");
        Duration window = properties.getWindow();
        if (window == null || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("security.rate-limit.window must be positive");
        }
        this.windowMillis = window.toMillis();
        if (windowMillis <= 0) {
            throw new IllegalArgumentException("security.rate-limit.window must be at least 1 millisecond");
        }
    }

    public boolean tryAcquire(String clientKey, long nowMillis) {
        cleanupExpiredCounters(nowMillis);
        AtomicBoolean allowed = new AtomicBoolean(true);
        counters.compute(clientKey, (key, current) -> {
            if (current == null || nowMillis < current.windowStartedAt()
                    || nowMillis - current.windowStartedAt() >= windowMillis) {
                if (current == null && counters.size() >= maxTrackedClients) {
                    allowed.set(false);
                    return null;
                }
                return new WindowCounter(nowMillis, 1);
            }
            if (current.requestCount() >= requestsPerWindow) {
                allowed.set(false);
                return current;
            }
            return new WindowCounter(current.windowStartedAt(), current.requestCount() + 1);
        });
        return allowed.get();
    }

    public long retryAfterSeconds() {
        return Math.max(1, (windowMillis + 999) / 1000);
    }

    int trackedClientCount() {
        return counters.size();
    }

    private void cleanupExpiredCounters(long nowMillis) {
        long scheduled = nextCleanupAt.get();
        if (nowMillis < scheduled || !nextCleanupAt.compareAndSet(scheduled, nowMillis + windowMillis)) {
            return;
        }
        counters.entrySet().removeIf(entry -> nowMillis < entry.getValue().windowStartedAt()
                || nowMillis - entry.getValue().windowStartedAt() >= windowMillis);
    }

    private int requirePositive(int value, String propertyName) {
        if (value <= 0) {
            throw new IllegalArgumentException("security.rate-limit." + propertyName + " must be positive");
        }
        return value;
    }

    private record WindowCounter(long windowStartedAt, int requestCount) {
    }
}
