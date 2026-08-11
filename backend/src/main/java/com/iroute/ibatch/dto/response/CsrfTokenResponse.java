package com.iroute.ibatch.dto.response;

public record CsrfTokenResponse(
        String headerName,
        String token) {
}
