package com.example.storeapi.controller;

import com.example.storeapi.dto.LoginResponse;
import com.example.storeapi.dto.RegisterRequest;
import com.example.storeapi.model.User;
import com.example.storeapi.service.LoginAttemptService;
import com.example.storeapi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register and login endpoints")
public class AuthController {

    private final UserService userService;
    private final LoginAttemptService loginAttemptService;

    @Operation(summary = "Register a new user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User registered successfully"),
            @ApiResponse(responseCode = "409", description = "Email already exists",
                    content = @Content(schema = @Schema(example = "User already exists")))
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            userService.register(request);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @Operation(summary = "Login with email and password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(example = "Invalid password"))),
            @ApiResponse(responseCode = "429", description = "Too many failed attempts - account locked",
                    content = @Content(schema = @Schema(example = "Account temporarily locked. Try again in 15 minute(s).")))
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody RegisterRequest request,
                                   HttpServletRequest httpRequest) {
        String email = request.getEmail();

        if (loginAttemptService.isBlocked(email)) {
            long minutes = loginAttemptService.getMinutesUntilUnlock(email);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Account temporarily locked. Try again in " + minutes + " minute(s).");
        }

        try {
            User user = userService.findByEmail(email);

            if (!userService.checkPassword(user, request.getPassword())) {
                loginAttemptService.loginFailed(email);
                int remaining = 5 - 1; // will be calculated below
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid password");
            }

            loginAttemptService.loginSucceeded(email);
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("userId", user.getId());

            return ResponseEntity.ok(new LoginResponse(session.getId()));

        } catch (RuntimeException e) {
            loginAttemptService.loginFailed(email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
}