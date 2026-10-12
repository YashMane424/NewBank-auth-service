package com.newbank.authservice.controller;

import com.newbank.authservice.dto.AuthResponse;
import com.newbank.authservice.dto.LoginRequest;
import com.newbank.authservice.dto.SignupRequest;
import com.newbank.authservice.service.AuthService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Timed(value = "auth.signup.endpoint", description = "Signup endpoint response time")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        log.info("POST /api/auth/signup - Signup endpoint hit for user: {}", request.getUsername());
        AuthResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Timed(value = "auth.login.endpoint", description = "Login endpoint response time")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/auth/login - Login endpoint hit for user: {}", request.getUsername());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Timed(value = "auth.refresh.endpoint", description = "Refresh token endpoint response time")
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody Map<String, @NotBlank String> request) {
        String refreshToken = request.get("refreshToken");
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    @Timed(value = "auth.validate.endpoint", description = "Token validation endpoint response time")
    public ResponseEntity<Map<String, Object>> validateToken(
            @Valid @RequestBody Map<String, @NotBlank String> request) {
        String token = request.get("token");
        boolean isValid = authService.validateToken(token);
        return ResponseEntity.ok(Map.of(
                "valid", isValid,
                "timestamp", System.currentTimeMillis()
        ));
    }

    @GetMapping("/health")
    @Timed(value = "auth.health.endpoint", description = "Health check endpoint response time")
    public ResponseEntity<Map<String, String>> health() {
        log.debug("Health check request received");
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "auth-service",
                "version", "1.0.0"
        ));
    }
}
