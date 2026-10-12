package com.newbank.authservice.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.refresh-key}")
    private String refreshKey;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    private Key getRefreshSigningKey() {
        return Keys.hmacShaKeyFor(refreshKey.getBytes());
    }

    public String generateToken(String username, Long userId) {
        return createToken(username, userId, expirationMs, getSigningKey());
    }

    public String generateRefreshToken(String username, Long userId) {
        return createToken(username, userId, refreshExpirationMs, getRefreshSigningKey());
    }

    private String createToken(String username, Long userId, long expirationTime, Key signingKey) {
        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + expirationTime;

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(new Date(nowMillis))
                .expiration(new Date(expMillis))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject, getSigningKey());
    }

    public Long extractUserIdFromToken(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class), getSigningKey());
    }

    public String extractUsernameFromRefreshToken(String token) {
        return extractClaim(token, Claims::getSubject, getRefreshSigningKey());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver, Key signingKey) {
        Claims claims = extractAllClaims(token, signingKey);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token, Key signingKey) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
            log.error("JWT parsing error: {}", e.getMessage());
            throw new RuntimeException("Invalid token", e);
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token, getSigningKey()).before(new Date());
        } catch (Exception e) {
            log.error("Token expiration check failed: {}", e.getMessage());
            return true;
        }
    }

    private Date extractExpiration(String token, Key signingKey) {
        return extractClaim(token, Claims::getExpiration, signingKey);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean validateRefreshToken(String token, String username) {
        try {
            String tokenUsername = extractUsernameFromRefreshToken(token);
            return tokenUsername.equals(username) && !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Refresh token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public LocalDateTime getExpirationDateTime(String token) {
        try {
            Date expiration = extractExpiration(token, getSigningKey());
            return expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception e) {
            log.error("Failed to extract expiration date: {}", e.getMessage());
            return null;
        }
    }
}
