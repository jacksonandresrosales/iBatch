package com.iroute.ibatch.dto.response;

public record ProcessFileResponse(
        Long fileId,
        String fileName,
        String status,
        String message) {
}
