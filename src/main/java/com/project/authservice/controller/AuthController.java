package com.project.NewBank.controller;

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

import com.project.NewBank.Security.request.LoginRequest;
import com.project.NewBank.Security.request.SignupRequest;
import com.project.NewBank.Service.Login.LoginService;
import com.project.NewBank.Service.Security.JwtService;
import com.project.NewBank.Service.Security.UserDetailsServiceImpl;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    LoginService loginService;
    
    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    JwtService jwtService;


    @PostMapping({"/login"})
    public ResponseEntity<?> login(@RequestBody @Validated LoginRequest loginRequest) {
        System.out.println("LOGIN CONTROLLER HIT");
        return ResponseEntity.ok(loginService.login(loginRequest));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody @Validated SignupRequest signupRequest) {
       
        return ResponseEntity.created(null).body(loginService.signUp(signupRequest));
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
