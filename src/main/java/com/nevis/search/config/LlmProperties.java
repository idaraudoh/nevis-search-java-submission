package com.nevis.search.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nevis.llm")
public record LlmProperties(String apiKey, String model, Duration timeout) {

    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
