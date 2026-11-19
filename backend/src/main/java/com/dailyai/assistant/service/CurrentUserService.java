package com.dailyai.assistant.service;

import com.dailyai.assistant.entity.User;
import com.dailyai.assistant.exception.BadRequestException;
import com.dailyai.assistant.exception.UnauthorizedException;
import com.dailyai.assistant.repository.UserRepository;
import com.dailyai.assistant.security.AppUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Resolves the owner of the current request from Spring Security. There is no
 * shared or cached profile: every request is scoped to its authenticated user.
 */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        return getCurrentUserOrNull()
                .orElseThrow(() -> new UnauthorizedException("Authentication is required."));
    }

    @Transactional(readOnly = true)
    public Optional<User> getCurrentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            return Optional.empty();
        }
        return userRepository.findById(principal.getId())
                .filter(User::isEnabled);
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public Long getCurrentUserIdOrNull() {
        return getCurrentUserOrNull().map(User::getId).orElse(null);
    }

    @Transactional
    public User updateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name must not be blank");
        }
        User user = getCurrentUser();
        user.setName(name.trim());
        return userRepository.save(user);
    }

    @Transactional
    public User updatePreferences(String preferences) {
        User user = getCurrentUser();
        user.setPreferences(preferences);
        return userRepository.save(user);
    }
}
