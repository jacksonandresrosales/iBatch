package com.iroute.ibatch.domain.model;

public record AppUser(
        Long id,
        String username,
        String passwordHash,
        UserRole role,
        boolean enabled) {
}
