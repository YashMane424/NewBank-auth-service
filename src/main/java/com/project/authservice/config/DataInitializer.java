package com.project.authservice.config;

import com.project.authservice.model.Role;
import com.project.authservice.model.User;
import com.project.authservice.model.Enum.RoleName;
import com.project.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    @Bean
    @Profile("local") // only runs when spring.profiles.active=local
    public CommandLineRunner seedTestUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            if (userRepository.findByUsername("testuser") != null) {
                log.info("Test user already exists — skipping seed");
                return;
            }

            User testUser = new User(
                "testuser",
                passwordEncoder.encode("password123"),
                "Test User",
                "test@example.com"
            );
            testUser.setRoles(Set.of(new Role(RoleName.ROLE_USER)));

            userRepository.save(testUser);
            log.info("✅ Test user seeded: username=testuser password=password123");
        };
    }
}
