package com.iroute.ibatch.dto.response;

import java.time.OffsetDateTime;

public record ApiHealthResponse(
        String status,
        String message,
        OffsetDateTime timestamp) {
}
