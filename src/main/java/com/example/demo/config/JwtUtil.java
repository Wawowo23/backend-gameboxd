package com.example.demo.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretSeed;



    public String generateToken(String uid, String name) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("uid", uid);
        extraClaims.put("name", name);

        SecretKey key = Keys.hmacShaKeyFor(secretSeed.getBytes(StandardCharsets.UTF_8));

        long duration = 3 * 24 * 60 * 60 * 1000;

        return Jwts.builder()
                .claims(extraClaims)
                .subject(uid)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + duration))
                .signWith(key)
                .compact();
    }

    public String extractUid(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secretSeed.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}