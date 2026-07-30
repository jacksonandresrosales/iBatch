package com.iroute.ibatch.dto.response;

public record ProcessFileResponse(
        String fileName,
        String status,
        String message) {
}
