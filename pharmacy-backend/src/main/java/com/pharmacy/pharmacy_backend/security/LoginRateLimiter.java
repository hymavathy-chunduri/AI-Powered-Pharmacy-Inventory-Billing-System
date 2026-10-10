package com.pharmacy.pharmacy_backend.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory brute-force protection rate limiter for employee logins.
 * Locks an identifier after 5 consecutive failed attempts for 15 minutes.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_SECONDS = 15 * 60; // 15 minutes

    private static class Attempt {
        int count;
        Instant firstAttemptAt;
        Instant lockedUntil;

        Attempt() {
            this.count = 1;
            this.firstAttemptAt = Instant.now();
            this.lockedUntil = null;
        }
    }

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public synchronized boolean isBlocked(String identifier) {
        if (identifier == null) return false;
        String key = identifier.toLowerCase().trim();
        Attempt attempt = attempts.get(key);
        if (attempt == null) {
            return false;
        }

        Instant now = Instant.now();
        if (attempt.lockedUntil != null) {
            if (now.isBefore(attempt.lockedUntil)) {
                return true;
            } else {
                // Lockout expired
                attempts.remove(key);
                return false;
            }
        }

        // Expire attempt count after lockout window
        if (now.isAfter(attempt.firstAttemptAt.plusSeconds(LOCKOUT_DURATION_SECONDS))) {
            attempts.remove(key);
            return false;
        }

        return false;
    }

    public synchronized void recordFailure(String identifier) {
        if (identifier == null) return;
        String key = identifier.toLowerCase().trim();
        Instant now = Instant.now();

        Attempt attempt = attempts.get(key);
        if (attempt == null || now.isAfter(attempt.firstAttemptAt.plusSeconds(LOCKOUT_DURATION_SECONDS))) {
            attempts.put(key, new Attempt());
        } else {
            attempt.count++;
            if (attempt.count >= MAX_FAILED_ATTEMPTS) {
                attempt.lockedUntil = now.plusSeconds(LOCKOUT_DURATION_SECONDS);
            }
        }
    }

    public synchronized void recordSuccess(String identifier) {
        if (identifier == null) return;
        attempts.remove(identifier.toLowerCase().trim());
    }

    public synchronized long getRemainingLockoutSeconds(String identifier) {
        if (identifier == null) return 0;
        Attempt attempt = attempts.get(identifier.toLowerCase().trim());
        if (attempt != null && attempt.lockedUntil != null) {
            long remaining = attempt.lockedUntil.getEpochSecond() - Instant.now().getEpochSecond();
            return Math.max(0, remaining);
        }
        return 0;
    }
}
