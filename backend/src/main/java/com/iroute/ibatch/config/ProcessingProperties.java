package com.iroute.ibatch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.processing")
public record ProcessingProperties(
        int batchSize,
        long maxRecords) {
}


