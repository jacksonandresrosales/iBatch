package com.iroute.ibatch.dto.response;

import java.time.OffsetDateTime;

public record ErrorResponse(
        String message,
        OffsetDateTime timestamp) {
}
