package com.cms.cms.config;

import com.cms.cms.model.User;
import com.cms.cms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first admin from environment variables (APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD).
 * Does nothing if they are not set or the admin already exists.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.name:Admin}")
    private String adminName;

    public AdminSeeder(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.info("No admin seeded (APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD not set).");
            return;
        }
        String email = adminEmail.trim().toLowerCase();
        if (users.findByEmail(email).isPresent()) {
            return;
        }
        User admin = new User();
        admin.setName(adminName);
        admin.setEmail(email);
        admin.setPassword(encoder.encode(adminPassword));
        admin.setRole("ADMIN");
        users.save(admin);
        log.info("Admin user created: {}", email);
    }
}
