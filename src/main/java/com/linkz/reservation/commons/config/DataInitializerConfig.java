package com.linkz.reservation.commons.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.linkz.reservation.user.User;
import com.linkz.reservation.user.UserRepository;

@Configuration
@Profile("dev")
@Slf4j
public class DataInitializerConfig {

    @Configuration
    @RequiredArgsConstructor
    public static class DemoDataInitializer implements CommandLineRunner {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;

        @Override
        public void run(String... args) throws Exception {
            // Check if demo user already exists
            if (userRepository.count() > 0) {
                log.info("Database already initialized, skipping demo data seeding");
                return;
            }

            log.info("Initializing development data...");

            // Create demo user
            User demoUser = User.builder()
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .build();

            userRepository.save(demoUser);
            log.info("✅ Demo user created: test@example.com / password123");
        }
    }
}

