package com.agent2026.interview.shared.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "security.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private int requestsPerWindow = 30;
    private Duration window = Duration.ofMinutes(1);
    private int maxTrackedClients = 10_000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRequestsPerWindow() {
        return requestsPerWindow;
    }

    public void setRequestsPerWindow(int requestsPerWindow) {
        this.requestsPerWindow = requestsPerWindow;
    }

    public Duration getWindow() {
        return window;
    }

    public void setWindow(Duration window) {
        this.window = window;
    }

    public int getMaxTrackedClients() {
        return maxTrackedClients;
    }

    public void setMaxTrackedClients(int maxTrackedClients) {
        this.maxTrackedClients = maxTrackedClients;
    }
}
