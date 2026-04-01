package com.booking.therapist_api.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/api/test-auth")
public class TestAuthController {

    @Value("${jwt.secret:your-fallback-secret-key-that-is-at-least-256-bits-long}")
    private String secret;

    @GetMapping("/token")
    public String generateToken(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "ROLE_USER") String role
    ) {
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + 60 * 60 * 1000);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .compact();
    }
}
