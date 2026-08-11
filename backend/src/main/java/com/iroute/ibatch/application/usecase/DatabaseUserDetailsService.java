package com.iroute.ibatch.application.usecase;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.iroute.ibatch.infrastructure.persistence.repository.UserRepository;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userRepository.findByUsername(username.strip().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales invalidas"));

        return User.withUsername(user.username())
                .password(user.passwordHash())
                .roles(user.role().name())
                .disabled(!user.enabled())
                .build();
    }
}
