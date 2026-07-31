package com.iroute.ibatch.dto.response;

import java.time.OffsetDateTime;

public record DatabaseHealthResponse(
        boolean connected,
        String message,
        String databaseProduct,
        String url,
        OffsetDateTime timestamp) {
}
