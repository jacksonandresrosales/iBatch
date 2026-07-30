package com.iroute.ibatch.dto.response;

import java.time.OffsetDateTime;

public record AvailableFileResponse(
        String fileName,
        long sizeBytes,
        OffsetDateTime lastModifiedAt,
        boolean expectedFormat) {
}
