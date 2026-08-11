package com.iroute.ibatch.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;

import com.iroute.ibatch.dto.request.LoginRequest;

class AuthControllerTest {

    @Test
    void shouldPersistAuthenticatedAdminSession() {
        var authenticationManager = mock(AuthenticationManager.class);
        var securityContextRepository = mock(SecurityContextRepository.class);
        var sessionAuthenticationStrategy = mock(SessionAuthenticationStrategy.class);
        var controller = new AuthController(
                authenticationManager,
                securityContextRepository,
                sessionAuthenticationStrategy);
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "admin",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);

        var result = controller.login(
                new LoginRequest("ADMIN", "contrasena-segura"),
                request,
                response);

        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().username()).isEqualTo("admin");
        assertThat(result.getBody().role()).isEqualTo("ADMIN");
        verify(sessionAuthenticationStrategy).onAuthentication(authentication, request, response);
        verify(securityContextRepository).saveContext(any(), any(), any());
    }
}
