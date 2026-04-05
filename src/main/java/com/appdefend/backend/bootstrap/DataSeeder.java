package com.appdefend.backend.bootstrap;

import com.appdefend.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        userRepository.findByEmail("admin@appdefend.local").ifPresentOrElse(
            user -> { },
            () -> {
                Long id = userRepository.create("Platform Admin", "admin@appdefend.local",
                    passwordEncoder.encode("Admin@123"), true);
                userRepository.assignRole(id, 1L);
            }
        );
    }
}
