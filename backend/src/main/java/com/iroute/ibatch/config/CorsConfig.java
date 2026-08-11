package com.iroute.ibatch.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        addFrontendCors(registry, "/api/**");
        addFrontendCors(registry, "/auth/**");
        addFrontendCors(registry, "/files");
        addFrontendCors(registry, "/files/**");
        addFrontendCors(registry, "/transactions/**");
        addFrontendCors(registry, "/dashboard/**");
        addFrontendCors(registry, "/dashboard");
        addFrontendCors(registry, "/logs/**");
        addFrontendCors(registry, "/logs");
    }

    private void addFrontendCors(CorsRegistry registry, String pathPattern) {
        registry.addMapping(pathPattern)
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept", "X-CSRF-TOKEN")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
