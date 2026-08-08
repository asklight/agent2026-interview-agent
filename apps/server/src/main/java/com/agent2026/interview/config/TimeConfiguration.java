package com.agent2026.interview.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfiguration {
    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
