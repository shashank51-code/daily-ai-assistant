package com.dailyai.assistant.config;

import com.dailyai.assistant.entity.Role;
import com.dailyai.assistant.entity.User;
import com.dailyai.assistant.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;

/**
 * Optional, environment-only activation of the pre-authentication profile.
 *
 * <p>No password is created by a migration and no default account is seeded.
 * When BOOTSTRAP_EMAIL and BOOTSTRAP_PASSWORD are both supplied, and the email
 * matches an existing user without a password, that user receives a BCrypt hash.
 * The credentials are never logged or committed. Existing passwords are never
 * overwritten.</p>
 */
@Component
public class LegacyAccountBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyAccountBootstrap.class);

    private final AppProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LegacyAccountBootstrap(AppProperties properties, UserRepository userRepository,
                                  PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = properties.getAuth().getBootstrapEmail();
        String password = properties.getAuth().getBootstrapPassword();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        if (!isStrong(password)) {
            throw new IllegalStateException(
                    "BOOTSTRAP_PASSWORD does not meet the application password policy.");
        }

        String normalized = email.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(normalized).orElse(null);
        if (user == null) {
            log.warn("Bootstrap account was configured but no matching existing user exists; nothing was changed.");
            return;
        }
        if (user.getPasswordHash() != null && !user.getPasswordHash().isBlank()) {
            log.info("Bootstrap account already has a password; the existing credential was kept.");
            return;
        }

        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(parseRole(properties.getAuth().getBootstrapRole()));
        user.setEnabled(true);
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);
        log.info("Existing profile was activated through the environment bootstrap.");
    }

    private boolean isStrong(String password) {
        return password.length() >= 10
                && password.length() <= 72
                && password.chars().anyMatch(Character::isLowerCase)
                && password.chars().anyMatch(Character::isUpperCase)
                && password.chars().anyMatch(Character::isDigit)
                && password.chars().anyMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c));
    }

    private Role parseRole(String value) {
        try {
            return Role.valueOf(value == null ? "USER" : value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warn("BOOTSTRAP_ROLE is invalid; using USER.");
            return Role.USER;
        }
    }
}
