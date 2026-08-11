package com.iroute.ibatch.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.iroute.ibatch.domain.model.UserRole;
import com.iroute.ibatch.infrastructure.persistence.repository.UserRepository;

class InitialUsersInitializerTest {

    @Test
    void createsAdminAndOperatorWhenTheyDoNotExist() {
        var userRepository = mock(UserRepository.class);
        var passwordEncoder = mock(PasswordEncoder.class);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin-password-123")).thenReturn("admin-hash");
        when(passwordEncoder.encode("operator-password-123")).thenReturn("operator-hash");

        var initializer = new InitialUsersInitializer(
                userRepository,
                passwordEncoder,
                "ADMIN",
                "admin-password-123",
                "OPERATOR",
                "operator-password-123");

        initializer.run(null);

        verify(userRepository).create("admin", "admin-hash", UserRole.ADMIN);
        verify(userRepository).create("operator", "operator-hash", UserRole.OPERATOR);
    }
}
