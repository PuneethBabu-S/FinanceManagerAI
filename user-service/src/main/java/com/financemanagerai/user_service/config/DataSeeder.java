package com.financemanagerai.user_service.config;

import com.financemanagerai.user_service.model.Role;
import com.financemanagerai.user_service.model.User;
import com.financemanagerai.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner seedAdmin() {
        return args -> {
            String adminEmail = "admin@financemanager.ai";
            if (!userRepository.existsByEmail(adminEmail)) {
                User admin = User.builder()
                        .email(adminEmail)
                        .password(passwordEncoder.encode("Admin@1234"))
                        .firstName("Admin")
                        .lastName("User")
                        .build();
                admin.getRoles().add(Role.ADMIN);
                admin.getRoles().add(Role.USER);
                userRepository.save(admin);
            }
        };
    }
}
