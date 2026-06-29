package com.voca.backend.enrichment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record EnrichmentProperties(
        int batchSize,
        int maxRetries
) {
    public EnrichmentProperties {
        if (batchSize <= 0) {
            batchSize = 20;
        }
        if (maxRetries <= 0) {
            maxRetries = 3;
        }
    }
}
