package com.splitit.infrastructure.config;

import com.splitit.domain.user.port.out.PasswordHasher;
import com.splitit.domain.user.port.out.UserRepository;
import com.splitit.domain.user.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires pure domain services (which carry no Spring annotations) as beans,
 * injecting their out-port adapters.
 */
@Configuration
public class DomainConfig {

    @Bean
    public UserService userService(UserRepository userRepository, PasswordHasher passwordHasher) {
        return new UserService(userRepository, passwordHasher);
    }
}
