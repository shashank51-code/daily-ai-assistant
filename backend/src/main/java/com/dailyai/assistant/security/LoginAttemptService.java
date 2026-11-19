package com.dailyai.assistant.security;

import com.dailyai.assistant.config.AppProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bounded per-instance throttling for repeated failed logins. A distributed
 * deployment should replace this with a shared store (for example Redis), but
 * this is deliberately dependency-free and resets automatically.
 */
@Service
public class LoginAttemptService {

    private final int maxAttempts;
    private final Duration window;
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(AppProperties properties) {
        this.maxAttempts = Math.max(1, properties.getAuth().getMaxLoginAttempts());
        this.window = Duration.ofMinutes(Math.max(1, properties.getAuth().getLoginWindowMinutes()));
    }

    public boolean isBlocked(String email, String clientAddress) {
        Attempt attempt = attempts.get(key(email, clientAddress));
        if (attempt == null) {
            return false;
        }
        if (attempt.expiresAt.isBefore(Instant.now())) {
            attempts.remove(key(email, clientAddress), attempt);
            return false;
        }
        return attempt.count >= maxAttempts;
    }

    public void recordFailure(String email, String clientAddress) {
        String key = key(email, clientAddress);
        Instant now = Instant.now();
        // Keep the map bounded under a flood of unique emails/addresses.
        if (attempts.size() > 10_000 && !attempts.containsKey(key)) {
            attempts.clear();
        }
        attempts.compute(key, (ignored, current) -> {
            if (current == null || current.expiresAt.isBefore(now)) {
                return new Attempt(1, now.plus(window));
            }
            return new Attempt(current.count + 1, current.expiresAt);
        });
    }

    public void reset(String email, String clientAddress) {
        attempts.remove(key(email, clientAddress));
    }

    private String key(String email, String clientAddress) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String address = clientAddress == null ? "unknown" : clientAddress;
        return normalizedEmail + "|" + address;
    }

    private record Attempt(int count, Instant expiresAt) {
    }
}
