package com.newbank.authservice.service;

import com.newbank.authservice.dto.AuthResponse;
import com.newbank.authservice.dto.LoginRequest;
import com.newbank.authservice.dto.SignupRequest;
import com.newbank.authservice.dto.UserSignupEvent;
import com.newbank.authservice.entity.User;
import com.newbank.authservice.exception.AuthenticationException;
import com.newbank.authservice.exception.UserAlreadyExistsException;
import com.newbank.authservice.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;
    private final MeterRegistry meterRegistry;

    private static final String SIGNUP_COUNTER = "auth.signup.total";
    private static final String LOGIN_COUNTER = "auth.login.total";
    private static final String LOGIN_FAILURE_COUNTER = "auth.login.failures";
    private static final String SIGNUP_TIMER = "auth.signup.duration";
    private static final String LOGIN_TIMER = "auth.login.duration";
    private static final String AUTH_EXCHANGE = "auth-exchange";
    private static final String SIGNUP_ROUTING_KEY = "auth.user.signup";
    private static final String LOGIN_ROUTING_KEY = "auth.user.login";

    @Transactional
    @CircuitBreaker(name = "authServiceCB", fallbackMethod = "signupFallback")
    @Retry(name = "authServiceRetry")
    @RateLimiter(name = "authServiceLimiter")
    public AuthResponse signup(SignupRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            log.info("Signup attempt for username: {}", request.getUsername());

            if (userRepository.existsByUsername(request.getUsername())) {
                throw new UserAlreadyExistsException("Username already exists");
            }

            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("Email already exists");
            }

            User user = User.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .fullname(request.getFullname())
                    .active(true)
                    .failedLoginAttempts(0)
                    .build();

            User savedUser = userRepository.save(user);
            log.info("User created successfully: {}", savedUser.getId());

            String token = jwtService.generateToken(savedUser.getUsername(), savedUser.getId());
            String refreshToken = jwtService.generateRefreshToken(savedUser.getUsername(), savedUser.getId());

            // Publish async event
            publishUserSignupEvent(savedUser);

            Counter.builder(SIGNUP_COUNTER)
                    .description("Total signup attempts")
                    .register(meterRegistry)
                    .increment();

            sample.stop(Timer.builder(SIGNUP_TIMER)
                    .description("Signup operation duration")
                    .tag("status", "success")
                    .register(meterRegistry));

            return AuthResponse.builder()
                    .token(token)
                    .refreshToken(refreshToken)
                    .userId(savedUser.getId())
                    .username(savedUser.getUsername())
                    .email(savedUser.getEmail())
                    .message("Signup successful")
                    .expiresAt(jwtService.getExpirationDateTime(token))
                    .build();
        } catch (Exception e) {
            log.error("Signup failed: {}", e.getMessage(), e);
            sample.stop(Timer.builder(SIGNUP_TIMER)
                    .description("Signup operation duration")
                    .tag("status", "failure")
                    .register(meterRegistry));
            throw e;
        }
    }

    @Transactional
    @CircuitBreaker(name = "authServiceCB", fallbackMethod = "loginFallback")
    @Retry(name = "authServiceRetry")
    @RateLimiter(name = "authServiceLimiter")
    public AuthResponse login(LoginRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            log.info("Login attempt for username: {}", request.getUsername());

            Optional<User> optionalUser = userRepository.findByUsernameWithLock(request.getUsername());

            if (optionalUser.isEmpty()) {
                optionalUser = userRepository.findByEmail(request.getUsername());
            }

            if (optionalUser.isEmpty()) {
                Counter.builder(LOGIN_FAILURE_COUNTER)
                        .description("Total login failures")
                        .register(meterRegistry)
                        .increment();
                throw new AuthenticationException("User not found");
            }

            User user = optionalUser.get();

            if (!user.getActive()) {
                throw new AuthenticationException("User account is inactive");
            }

            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
                throw new AuthenticationException("User account is locked");
            }

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                if (user.getFailedLoginAttempts() >= 5) {
                    user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
                    log.warn("User {} locked due to failed login attempts", user.getUsername());
                }
                userRepository.save(user);
                Counter.builder(LOGIN_FAILURE_COUNTER)
                        .description("Total login failures")
                        .register(meterRegistry)
                        .increment();
                throw new AuthenticationException("Invalid password");
            }

            // Reset failed attempts on successful login
            user.setFailedLoginAttempts(0);
            user.setLastLoginAt(LocalDateTime.now());
            user.setLockedUntil(null);
            userRepository.save(user);

            String token = jwtService.generateToken(user.getUsername(), user.getId());
            String refreshToken = jwtService.generateRefreshToken(user.getUsername(), user.getId());

            // Publish async login event
            publishLoginAuditEvent(user);

            Counter.builder(LOGIN_COUNTER)
                    .description("Total successful logins")
                    .register(meterRegistry)
                    .increment();

            sample.stop(Timer.builder(LOGIN_TIMER)
                    .description("Login operation duration")
                    .tag("status", "success")
                    .register(meterRegistry));

            return AuthResponse.builder()
                    .token(token)
                    .refreshToken(refreshToken)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .message("Login successful")
                    .expiresAt(jwtService.getExpirationDateTime(token))
                    .build();
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            sample.stop(Timer.builder(LOGIN_TIMER)
                    .description("Login operation duration")
                    .tag("status", "failure")
                    .register(meterRegistry));
            throw e;
        }
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        try {
            String username = jwtService.extractUsernameFromRefreshToken(refreshToken);
            Long userId = jwtService.extractUserIdFromToken(refreshToken);

            if (!jwtService.validateRefreshToken(refreshToken, username)) {
                throw new AuthenticationException("Invalid or expired refresh token");
            }

            String newAccessToken = jwtService.generateToken(username, userId);
            String newRefreshToken = jwtService.generateRefreshToken(username, userId);

            return AuthResponse.builder()
                    .token(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .userId(userId)
                    .username(username)
                    .message("Token refreshed successfully")
                    .expiresAt(jwtService.getExpirationDateTime(newAccessToken))
                    .build();
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw new AuthenticationException("Token refresh failed");
        }
    }

    @Cacheable(value = "tokenValidation", key = "#token", unless = "#result == false")
    public boolean validateToken(String token) {
        return jwtService.validateToken(token);
    }

    @Async("authExecutor")
    public CompletableFuture<Void> publishUserSignupEvent(User user) {
        try {
            UserSignupEvent event = UserSignupEvent.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .fullname(user.getFullname())
                    .signupAt(LocalDateTime.now())
                    .eventId(UUID.randomUUID().toString())
                    .build();

            rabbitTemplate.convertAndSend(
                    AUTH_EXCHANGE,
                    SIGNUP_ROUTING_KEY,
                    event
            );
            log.info("User signup event published: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to publish signup event: {}", e.getMessage(), e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Async("authExecutor")
    public CompletableFuture<Void> publishLoginAuditEvent(User user) {
        try {
            rabbitTemplate.convertAndSend(
                    AUTH_EXCHANGE,
                    LOGIN_ROUTING_KEY,
                    user.getUsername() + " logged in at " + LocalDateTime.now()
            );
            log.info("Login audit event published for user: {}", user.getUsername());
        } catch (Exception e) {
            log.error("Failed to publish login audit event: {}", e.getMessage(), e);
        }
        return CompletableFuture.completedFuture(null);
    }

    // Fallback methods
    public AuthResponse signupFallback(SignupRequest request, Exception ex) {
        log.error("Signup circuit breaker fallback triggered: {}", ex.getMessage());
        throw new RuntimeException("Signup service is temporarily unavailable", ex);
    }

    public AuthResponse loginFallback(LoginRequest request, Exception ex) {
        log.error("Login circuit breaker fallback triggered: {}", ex.getMessage());
        throw new RuntimeException("Login service is temporarily unavailable", ex);
    }
}
