package com.example.storeapi.controller;

import com.example.storeapi.dto.LoginResponse;
import com.example.storeapi.dto.RegisterRequest;
import com.example.storeapi.exception.InvalidCredentialsException;
import com.example.storeapi.exception.TooManyRequestsException;
import com.example.storeapi.model.User;
import com.example.storeapi.service.LoginAttemptService;
import com.example.storeapi.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final LoginAttemptService loginAttemptService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody RegisterRequest request,
                                               HttpServletRequest httpRequest) {
        String email = request.getEmail();

        if (loginAttemptService.isBlocked(email)) {
            long minutes = loginAttemptService.getMinutesUntilUnlock(email);
            throw new TooManyRequestsException(
                    "Account temporarily locked. Try again in " + minutes + " minute(s)."
            );
        }

        User user = userService.findByEmail(email);

        if (!userService.checkPassword(user, request.getPassword())) {
            loginAttemptService.loginFailed(email);
            throw new InvalidCredentialsException();
        }

        loginAttemptService.loginSucceeded(email);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("userId", user.getId());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of());

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);

        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext
        );

        return ResponseEntity.ok(new LoginResponse(session.getId()));
    }
}