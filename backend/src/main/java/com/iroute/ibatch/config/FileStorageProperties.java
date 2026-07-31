package com.iroute.ibatch.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.files")
public record FileStorageProperties(Path inputDir) {
}
