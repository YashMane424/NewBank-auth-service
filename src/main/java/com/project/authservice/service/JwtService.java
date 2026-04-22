package com.project.NewBank.Service.Security;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    @Autowired
    private UserDetailsService detailsService;

    @Value("${jwt.secret-key}")
    private String SECRET_KEY;

    @Value("${jwt.refresh-key}")
    private String REFRESH_KEY;



    private Key secretKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    private Key refreshKey() {
        return Keys.hmacShaKeyFor(REFRESH_KEY.getBytes());
    }

    private <T> T extractClaims(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
        .setSigningKey(secretKey())
        .build()
        .parseClaimsJws(token)
        .getBody();
    }

    private String extractUsername(String token) {
        return extractClaims(token, Claims::getSubject);
    }

    public String generateToken(Map<String, Object> map, UserDetails userDetails) {
        UserDetails userFromService;
        try {
            userFromService = detailsService.loadUserByUsername(userDetails.getUsername());
        } catch (UsernameNotFoundException e) {
            throw new RuntimeException("User not found", e);
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userFromService.getAuthorities().stream().collect(Collectors.toList()));
        return createToken(claims, userDetails);
    }

    public String createToken(Map<String, Object> claims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000))
                .signWith(secretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(Map<String, Object> claims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000))
                .signWith(refreshKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenExpired(String token) {
        Date expiration = extractClaims(token, Claims::getExpiration);
        return expiration.before(new Date());
    }

    public String extractUsernameFromToken(String token) {
        return extractUsername(token);
    }

    public String extractUsernameFromRefreshToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(refreshKey())
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }

    public boolean validateToken(String token, UserDetails user) {
        return user.getUsername().equals(extractUsername(token)) && !isTokenExpired(token);
    }

    public boolean validateRefreshToken(String token, String username) {
        try {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(refreshKey())
            .build()
            .parseClaimsJws(token)
            .getBody();

        String extractedUsername = claims.getSubject();
        Date expiration = claims.getExpiration();

        return extractedUsername.equals(username) && 
               !expiration.before(new Date());
    } catch (Exception e) {
        return false;
    }
    }
}
