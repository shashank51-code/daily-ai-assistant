package com.dailyai.assistant.service;

import com.dailyai.assistant.entity.User;
import com.dailyai.assistant.exception.ResourceNotFoundException;
import com.dailyai.assistant.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The application runs in single-profile mode: every request belongs to the first user
 * created by the migration. This keeps the project small while still persisting progress,
 * bookmarks and history per user in PostgreSQL.
 */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;
    private User cachedUser;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        if (cachedUser != null) {
            return cachedUser;
        }
        cachedUser = userRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new ResourceNotFoundException("No user profile found. Run the database migrations."));
        return cachedUser;
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    @Transactional
    public User updateName(String name) {
        if (name == null || name.isBlank()) {
            throw new com.dailyai.assistant.exception.BadRequestException("Name must not be blank");
        }
        User user = getCurrentUser();
        user.setName(name.trim());
        return userRepository.save(user);
    }
}
