package com.example.storeapi.controller;

import com.example.storeapi.dto.LoginResponse;
import com.example.storeapi.dto.RegisterRequest;
import com.example.storeapi.model.User;
import com.example.storeapi.service.LoginAttemptService;
import com.example.storeapi.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testUser;
    private RegisterRequest registerRequest;
    private RegisterRequest loginRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashed_password");

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("password123");

        loginRequest = new RegisterRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");
    }

    @Test
    void register_success_returns200() throws Exception {
        when(userService.register(any(RegisterRequest.class))).thenAnswer(i -> {
            User u = new User();
            u.setId(2L);
            u.setEmail("newuser@example.com");
            return u;
        });

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));

        verify(userService).register(any(RegisterRequest.class));
    }

    @Test
    void register_emailAlreadyExists_returns409() throws Exception {
        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("User already exists"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict())
                .andExpect(content().string("User already exists"));
    }

    @Test
    void login_success_returns200AndSessionId() throws Exception {
        when(loginAttemptService.isBlocked("test@example.com")).thenReturn(false);
        when(userService.findByEmail("test@example.com")).thenReturn(testUser);
        when(userService.checkPassword(testUser, "password123")).thenReturn(true);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.sessionId").isNotEmpty());

        verify(loginAttemptService).loginSucceeded("test@example.com");
    }

    @Test
    void login_invalidPassword_returns401() throws Exception {
        when(loginAttemptService.isBlocked("test@example.com")).thenReturn(false);
        when(userService.findByEmail("test@example.com")).thenReturn(testUser);
        when(userService.checkPassword(testUser, "wrong_password")).thenReturn(false);

        RegisterRequest wrongPasswordRequest = new RegisterRequest();
        wrongPasswordRequest.setEmail("test@example.com");
        wrongPasswordRequest.setPassword("wrong_password");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongPasswordRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid password"));

        verify(loginAttemptService).loginFailed("test@example.com");
    }

    @Test
    void login_userNotFound_returns401() throws Exception {
        when(loginAttemptService.isBlocked("test@example.com")).thenReturn(false);
        when(userService.findByEmail("test@example.com"))
                .thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("User not found"));

        verify(loginAttemptService).loginFailed("test@example.com");
    }

    @Test
    void login_accountLocked_returns429() throws Exception {
        when(loginAttemptService.isBlocked("test@example.com")).thenReturn(true);
        when(loginAttemptService.getMinutesUntilUnlock("test@example.com")).thenReturn(10L);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string("Account temporarily locked. Try again in 10 minute(s)."));

        verify(userService, never()).findByEmail(any());
    }

    @Test
    void login_setsSessionAttributes() throws Exception {
        when(loginAttemptService.isBlocked("test@example.com")).thenReturn(false);
        when(userService.findByEmail("test@example.com")).thenReturn(testUser);
        when(userService.checkPassword(testUser, "password123")).thenReturn(true);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        HttpSession session = result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getAttribute("userId")).isEqualTo(1L);
    }
}
