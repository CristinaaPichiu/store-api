package com.example.storeapi.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final Map<String, Integer> attempts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lockoutTime = new ConcurrentHashMap<>();

    public void loginFailed(String email) {
        int current = attempts.getOrDefault(email, 0);
        attempts.put(email, current + 1);

        if (current + 1 >= MAX_ATTEMPTS) {
            lockoutTime.put(email, LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
        }
    }

    public void loginSucceeded(String email) {
        attempts.remove(email);
        lockoutTime.remove(email);
    }

    public boolean isBlocked(String email) {
        if (!lockoutTime.containsKey(email)) {
            return false;
        }

        if (LocalDateTime.now().isAfter(lockoutTime.get(email))) {
            attempts.remove(email);
            lockoutTime.remove(email);
            return false;
        }

        return true;
    }

    public long getMinutesUntilUnlock(String email) {
        if (!lockoutTime.containsKey(email)) return 0;
        return java.time.Duration.between(LocalDateTime.now(), lockoutTime.get(email)).toMinutes() + 1;
    }
}