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
public class InitialUsersInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitialUsersInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;
    private final String operatorUsername;
    private final String operatorPassword;

    public InitialUsersInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.bootstrap-admin.username:admin}") String adminUsername,
            @Value("${app.security.bootstrap-admin.password:}") String adminPassword,
            @Value("${app.security.bootstrap-operator.username:operator}") String operatorUsername,
            @Value("${app.security.bootstrap-operator.password:}") String operatorPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = normalize(adminUsername);
        this.adminPassword = adminPassword;
        this.operatorUsername = normalize(operatorUsername);
        this.operatorPassword = operatorPassword;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        createIfMissing(adminUsername, adminPassword, UserRole.ADMIN, "IBATCH_ADMIN");
        createIfMissing(operatorUsername, operatorPassword, UserRole.OPERATOR, "IBATCH_OPERATOR");
    }

    private void createIfMissing(String username, String password, UserRole role, String variablePrefix) {
        if (password.isBlank()) {
            LOGGER.warn("{}_PASSWORD no esta definida; no se creara el usuario {} inicial", variablePrefix, role);
            return;
        }
        if (!username.matches("[a-z0-9._-]{3,100}")) {
            throw new IllegalStateException(variablePrefix + "_USERNAME no tiene un formato valido");
        }
        if (password.length() < 12 || password.length() > 72) {
            throw new IllegalStateException(variablePrefix + "_PASSWORD debe tener entre 12 y 72 caracteres");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }

        userRepository.create(username, passwordEncoder.encode(password), role);
        LOGGER.info("Usuario inicial creado: {} ({})", username, role);
    }

    private String normalize(String username) {
        return username.strip().toLowerCase();
    }
}
