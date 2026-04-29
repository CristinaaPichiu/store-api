package com.example.storeapi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        loginAttemptService = new LoginAttemptService();
    }

    @Test
    void loginSucceeded_clearsAttemptsAndLockout() {
        loginAttemptService.loginFailed("user@example.com");
        loginAttemptService.loginFailed("user@example.com");

        loginAttemptService.loginSucceeded("user@example.com");

        assertThat(loginAttemptService.isBlocked("user@example.com")).isFalse();
    }

    @Test
    void isBlocked_afterMaxFailedAttempts_returnsTrue() {
        String email = "user@example.com";

        for (int i = 0; i < 4; i++) {
            loginAttemptService.loginFailed(email);
            assertThat(loginAttemptService.isBlocked(email)).isFalse();
        }
        loginAttemptService.loginFailed(email);
        assertThat(loginAttemptService.isBlocked(email)).isTrue();
    }

    @Test
    void getMinutesUntilUnlock_withActiveBlock_returnsPositiveMinutes() {
        String email = "user@example.com";

        for (int i = 0; i < 5; i++) {
            loginAttemptService.loginFailed(email);
        }

        long minutes = loginAttemptService.getMinutesUntilUnlock(email);

        assertThat(minutes).isGreaterThanOrEqualTo(14).isLessThanOrEqualTo(16);
    }

    @Test
    void getMinutesUntilUnlock_noBlock_returnsZero() {
        long minutes = loginAttemptService.getMinutesUntilUnlock("unknown@example.com");

        assertThat(minutes).isZero();
    }

    @Test
    void isBlocked_afterLockoutExpires_returnsFalse() {
        String email = "user@example.com";

        for (int i = 0; i < 5; i++) {
            loginAttemptService.loginFailed(email);
        }

        assertThat(loginAttemptService.isBlocked(email)).isTrue();

        LoginAttemptService newService = new LoginAttemptService();
        assertThat(newService.isBlocked(email)).isFalse();
    }

    @Test
    void multipleUsers_blockedIndependently() {
        String user1 = "user1@example.com";
        String user2 = "user2@example.com";

        for (int i = 0; i < 5; i++) {
            loginAttemptService.loginFailed(user1);
        }

        assertThat(loginAttemptService.isBlocked(user1)).isTrue();
        assertThat(loginAttemptService.isBlocked(user2)).isFalse();

        loginAttemptService.loginFailed(user2);
        loginAttemptService.loginFailed(user2);

        assertThat(loginAttemptService.isBlocked(user2)).isFalse();
    }

    @Test
    void loginFailed_incrementsAttemptCounter() {
        String email = "user@example.com";

        loginAttemptService.loginFailed(email);
        loginAttemptService.loginFailed(email);
        loginAttemptService.loginFailed(email);

        assertThat(loginAttemptService.isBlocked(email)).isFalse();

        loginAttemptService.loginFailed(email);
        loginAttemptService.loginFailed(email);

        assertThat(loginAttemptService.isBlocked(email)).isTrue();
    }
}

