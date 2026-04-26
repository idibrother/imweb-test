package com.imweb.shop.auth.application;

import com.imweb.shop.global.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class LoginAttemptService {

    private record Attempt(int count, Instant lockUntil) {}

    private final ConcurrentHashMap<String, Attempt> cache = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long lockoutSeconds;

    public LoginAttemptService(AppProperties props) {
        this.maxAttempts = props.getSecurity().getMaxLoginAttempts();
        this.lockoutSeconds = props.getSecurity().getLockoutDurationMinutes() * 60L;
    }

    public boolean isBlocked(String key) {
        Attempt attempt = cache.get(key);
        if (attempt == null) return false;
        if (attempt.lockUntil() != null) {
            if (Instant.now().isBefore(attempt.lockUntil())) return true;
            cache.remove(key);
        }
        return false;
    }

    public void recordFailure(String key) {
        cache.compute(key, (k, v) -> {
            int count = (v == null ? 0 : v.count()) + 1;
            Instant lockUntil = count >= maxAttempts ? Instant.now().plusSeconds(lockoutSeconds) : null;
            return new Attempt(count, lockUntil);
        });
        Attempt attempt = cache.get(key);
        if (attempt != null && attempt.lockUntil() != null) {
            log.warn("Account locked after {} failed attempts: {}", attempt.count(), key);
        }
    }

    public void recordSuccess(String key) {
        cache.remove(key);
    }
}
