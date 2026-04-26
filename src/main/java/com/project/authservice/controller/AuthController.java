package com.project.authservice.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.authservice.request.LoginRequest;
import com.project.authservice.request.SignupRequest;
import com.project.authservice.service.AuthService;
import com.project.authservice.service.JwtService;
import com.project.authservice.service.UserDetailsServiceImpl;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    AuthService authService;
    
    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    JwtService jwtService;


    @PostMapping({"/login"})
    public ResponseEntity<?> login(@RequestBody @Validated LoginRequest loginRequest) {
        System.out.println("LOGIN CONTROLLER HIT");
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody @Validated SignupRequest signupRequest) {
       
        return ResponseEntity.created(null).body(authService.signUp(signupRequest));
    }

        @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");

        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(401).body("Refresh token missing");
        }

        try {
            String username = jwtService.extractUsernameFromRefreshToken(refreshToken);

            if (!jwtService.validateRefreshToken(refreshToken, username)) {
                return ResponseEntity.status(401).body("Invalid or expired refresh token");
            }

            String newAccessToken = jwtService.generateToken(
                new HashMap<>(),
                userDetailsService.loadUserByUsername(username)
            );

            return ResponseEntity.ok(Map.of("token", newAccessToken));

        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(401).body("Refresh failed: " + e.getMessage());
        }
    }
    
}
