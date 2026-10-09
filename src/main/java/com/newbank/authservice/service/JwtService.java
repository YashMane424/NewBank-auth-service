package com.newbank.authservice.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Service
@Slf4j
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

    public String generateToken(String username) {
        return createToken(username, expirationMs, getSigningKey());
    }

    public String generateRefreshToken(String username) {
        return createToken(username, refreshExpirationMs, getRefreshSigningKey());
    }

    private String createToken(String username, long expirationTime, Key signingKey) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject, getSigningKey());
    }

    public String extractUsernameFromRefreshToken(String token) {
        return extractClaim(token, Claims::getSubject, getRefreshSigningKey());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver, Key signingKey) {
        Claims claims = extractAllClaims(token, signingKey);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token, Key signingKey) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token, getSigningKey()).before(new Date());
    }

    private Date extractExpiration(String token, Key signingKey) {
        return extractClaim(token, Claims::getExpiration, signingKey);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean validateRefreshToken(String token, String username) {
        String tokenUsername = extractUsernameFromRefreshToken(token);
        return (tokenUsername.equals(username)) && !isTokenExpired(token);
    }
}
