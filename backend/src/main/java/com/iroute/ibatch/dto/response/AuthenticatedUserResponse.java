package com.iroute.ibatch.dto.response;

public record AuthenticatedUserResponse(
        String username,
        String role) {
}
