package com.iroute.ibatch.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.iroute.ibatch.domain.model.UserRole;
import com.iroute.ibatch.infrastructure.persistence.repository.UserRepository;

@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public AdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.bootstrap-admin.username:admin}") String username,
            @Value("${app.security.bootstrap-admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username.strip().toLowerCase();
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (password.isBlank()) {
            LOGGER.warn("IBATCH_ADMIN_PASSWORD no esta definida; no se creara el administrador inicial");
            return;
        }
        if (!username.matches("[a-z0-9._-]{3,100}")) {
            throw new IllegalStateException("IBATCH_ADMIN_USERNAME no tiene un formato valido");
        }
        if (password.length() < 12 || password.length() > 72) {
            throw new IllegalStateException("IBATCH_ADMIN_PASSWORD debe tener entre 12 y 72 caracteres");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }

        userRepository.create(username, passwordEncoder.encode(password), UserRole.ADMIN);
        LOGGER.info("Usuario administrador inicial creado: {}", username);
    }
}
