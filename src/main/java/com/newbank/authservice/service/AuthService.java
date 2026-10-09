package com.newbank.authservice.service;

import com.newbank.authservice.dto.AuthResponse;
import com.newbank.authservice.dto.LoginRequest;
import com.newbank.authservice.dto.SignupRequest;
import com.newbank.authservice.entity.User;
import com.newbank.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse signup(SignupRequest request) {
        log.info("Signup attempt for username: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullname(request.getFullname())
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(request.getUsername());
        String refreshToken = jwtService.generateRefreshToken(request.getUsername());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .email(user.getEmail())
                .message("Signup successful")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for username: {}", request.getUsername());

        Optional<User> optionalUser = userRepository.findByUsername(request.getUsername());

        if (optionalUser.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = optionalUser.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtService.generateToken(request.getUsername());
        String refreshToken = jwtService.generateRefreshToken(request.getUsername());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .email(user.getEmail())
                .message("Login successful")
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        log.info("Refreshing token");

        String username = jwtService.extractUsernameFromRefreshToken(refreshToken);

        if (!jwtService.validateRefreshToken(refreshToken, username)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        String newAccessToken = jwtService.generateToken(username);
        String newRefreshToken = jwtService.generateRefreshToken(username);

        return AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .username(username)
                .message("Token refreshed successfully")
                .build();
    }

    public boolean validateToken(String token) {
        return jwtService.validateToken(token);
    }
}
