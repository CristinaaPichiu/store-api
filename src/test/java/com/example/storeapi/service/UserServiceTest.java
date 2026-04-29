package com.example.storeapi.service;

import com.example.storeapi.dto.RegisterRequest;
import com.example.storeapi.model.User;
import com.example.storeapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User existingUser;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("test@example.com");
        existingUser.setPasswordHash("hashed_password");

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("password123");
    }

    @Test
    void register_success() {
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(2L);
            return u;
        });

        User registered = userService.register(registerRequest);

        assertThat(registered).isNotNull();
        assertThat(registered.getId()).isEqualTo(2L);
        assertThat(registered.getEmail()).isEqualTo("newuser@example.com");
        assertThat(registered.getPasswordHash()).isEqualTo("hashed_password");
        verify(userRepository).existsByEmail("newuser@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_emailAlreadyExists_throws() {
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(registerRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User already exists");
    }

    @Test
    void findByEmail_found() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));

        User found = userService.findByEmail("test@example.com");

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(1L);
        assertThat(found.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void findByEmail_notFound_throws() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByEmail("unknown@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }

    @Test
    void checkPassword_correctPassword_returnsTrue() {
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);

        boolean matches = userService.checkPassword(existingUser, "password123");

        assertThat(matches).isTrue();
    }

    @Test
    void checkPassword_incorrectPassword_returnsFalse() {
        when(passwordEncoder.matches("wrong_password", "hashed_password")).thenReturn(false);

        boolean matches = userService.checkPassword(existingUser, "wrong_password");

        assertThat(matches).isFalse();
    }
}

